package br.ufc.arquivo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

// TODO: passar um objeto connection para o objeto database - IoC/dependency injection
// ver: https://github.com/abevieiramota/learning-spring-mvc/blob/master/src/com/abevieiramota/springmvc/model/ContatoDAO.java
public class CopyOfDatabase {

	private int chunkSize = 1000;

	private static final String QUERY_CREATE_TABLE = "create table pessoa (nome varchar(50), "
			+ "idade integer, profissão varchar(50), data_nascimento varchar(10))";

	private static final int NUMBER_COLUMNS_DATABASE = 4;

	private static final String QUERY_INSERT = "insert into pessoa values (?, ?, ?, ?);";

	private static final String QUERY_SELECT_ALL = "select * from pessoa";

	private static final String QUERY_DELETE_ALL = "delete from pessoa";

	private static final String QUERY_COUNT = "select count(*) from pessoa";

	private String url;

	public CopyOfDatabase(String url) {

		this.url = url;
	}

	public void reset() throws SQLException {

		try (Connection conn = DriverManager.getConnection(this.url);
				Statement stmt = conn.createStatement()) {

			stmt.execute(QUERY_DELETE_ALL);
		}
	}

	public void criarTabela() throws SQLException {

		try (Connection conn = DriverManager.getConnection(this.url);
				Statement stmt = conn.createStatement()) {

			stmt.execute(QUERY_CREATE_TABLE);
		}
	}

	public List<Object[]> all() throws SQLException {

		List<Object[]> pessoas = null;

		try (Connection conn = DriverManager.getConnection(this.url);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(QUERY_SELECT_ALL)) {

			pessoas = new ArrayList<Object[]>();

			while (rs.next()) {

				// olhar rs.getMetadata() -> descobrir a quantidade de colunas
				Object[] row = new Object[NUMBER_COLUMNS_DATABASE];

				pessoas.add(row);
				
				for (int i = 1; i <= row.length; i++) {

					if (i == 2) {

						row[i - 1] = rs.getInt(i);

						if (rs.wasNull()) {
							row[i - 1] = null;
						}

					} else {
						row[i - 1] = rs.getString(i);
					}
				}

			}

		}

		return pessoas;

	}

	// esperada list de String[] com length igual a x
	// de forma que será criado registro na tabela preenchendo as x primeiras
	// colunas
	// ficando as demais com valor null
	public void salvar(List<String[]> data) throws SQLException {

		try (Connection conn = DriverManager.getConnection(this.url);
				PreparedStatement stmt = conn.prepareStatement(QUERY_INSERT)) {

			try {

				conn.setAutoCommit(false);

				int batchSize = 0;

				for (String[] dataRow : data) {

					int colsPreenchidas = 0;

					while (colsPreenchidas < dataRow.length) {

						if (colsPreenchidas == 1) {

							if (dataRow[colsPreenchidas].isEmpty()) {
								stmt.setNull(colsPreenchidas + 1, Types.INTEGER);
							} else {

								stmt.setInt(colsPreenchidas + 1, Integer
										.parseInt(dataRow[colsPreenchidas]));
							}

						} else {
							stmt.setString(colsPreenchidas + 1,
									dataRow[colsPreenchidas]);
						}

						colsPreenchidas++;
					}

					while (colsPreenchidas < NUMBER_COLUMNS_DATABASE) {

						stmt.setNull(colsPreenchidas + 1, Types.VARCHAR);
						colsPreenchidas++;
					}

					stmt.addBatch();
					batchSize++;

					if (batchSize == this.chunkSize) {

						stmt.executeBatch();
						batchSize = 0;
					}
				}

				if (batchSize > 0) { // necessário, pois o driver do hsqldb
										// lança exceção caso seja chamado
					// executeBatch sem nenhum addBatch antes

					stmt.executeBatch();
				}

				conn.commit();

			} catch (SQLException e) {

				conn.rollback();
				throw e;
			}

		}
	}

	public Integer quantidadeDeRegistros() throws SQLException {

		Integer counter = null;

		try (Connection conn = DriverManager.getConnection(this.url);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(QUERY_COUNT)) {

			rs.next();
			counter = rs.getInt(1);
		}

		return counter;
	} // end quantidadeDeRegistros method

	public void setChunkSize(int size) {

		this.chunkSize = size;
	} // end setChunckSize method

} // end Database class