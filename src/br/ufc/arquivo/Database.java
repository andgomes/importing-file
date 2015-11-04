package br.ufc.arquivo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Database {

	private static final int COLUNA_DATA_NASCIMENTO = 3;

	private static final int COLUNA_IDADE = 1;

	private int chunkSize = 1000;

	private static final String QUERY_CREATE_TABLE = "create table pessoa (nome varchar(50), "
			+ "idade integer, profissão varchar(50), data_nascimento varchar(10))";

	private static final int NUMBER_COLUMNS_DATABASE = 4;

	private static final String QUERY_INSERT = "insert into pessoa values (?, ?, ?, ?);";

	private static final String QUERY_SELECT_ALL = "select * from pessoa";

	private static final String QUERY_DELETE_ALL = "delete from pessoa";

	private static final String QUERY_COUNT = "select count(*) from pessoa";

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");

	private Connection conn;

	public Database(Connection conn) {

		this.conn = conn;
	}

	public void reset() throws SQLException {

		try (Statement stmt = this.conn.createStatement()) {

			stmt.execute(QUERY_DELETE_ALL);
		}
	}

	public void criarTabela() throws SQLException {

		try (Statement stmt = this.conn.createStatement()) {

			stmt.execute(QUERY_CREATE_TABLE);
		}
	}

	public List<Object[]> all() throws SQLException {

		List<Object[]> pessoas = null;

		try (Statement stmt = this.conn.createStatement();
				ResultSet rs = stmt.executeQuery(QUERY_SELECT_ALL)) {

			pessoas = new ArrayList<Object[]>();

			while (rs.next()) {

				Object[] row = new Object[NUMBER_COLUMNS_DATABASE];
				
				row[0] = rs.getString(1);
				row[1] = rs.getInt(2);
				if(rs.wasNull()) {
					row[1] = null;
				}
				row[2] = rs.getString(3);
				row[3] = rs.getDate(4);

				pessoas.add(row);
			}

		}

		return pessoas;

	}

	// esperada list de String[] com length igual a x
	// de forma que será criado registro na tabela preenchendo as x primeiras
	// colunas
	// ficando as demais com valor null
	public void salvar(List<String[]> data) throws SQLException, ParseException {

		try (PreparedStatement pstmt = this.conn.prepareStatement(QUERY_INSERT)) {

			try {

				conn.setAutoCommit(false);

				int batchSize = 0;

				for (String[] dataRow : data) {

					int colsPreenchidas = 0;

					while (colsPreenchidas < dataRow.length) {

						if (colsPreenchidas == COLUNA_IDADE) {

							if (dataRow[colsPreenchidas].isEmpty()) {
								pstmt.setNull(colsPreenchidas + 1,
										Types.INTEGER);
							} else {

								pstmt.setInt(colsPreenchidas + 1, Integer
										.parseInt(dataRow[colsPreenchidas]));
							}

						} else if (colsPreenchidas == COLUNA_DATA_NASCIMENTO) {

							if (dataRow[colsPreenchidas].isEmpty()) {

								pstmt.setNull(colsPreenchidas + 1, Types.DATE);
							} else {

								pstmt.setDate(colsPreenchidas + 1, new Date(sdf
										.parse(dataRow[colsPreenchidas])
										.getTime()));
							}

						} else {
							pstmt.setString(colsPreenchidas + 1,
									dataRow[colsPreenchidas]);
						}

						colsPreenchidas++;
					}

					while (colsPreenchidas < NUMBER_COLUMNS_DATABASE) {

						pstmt.setNull(colsPreenchidas + 1, Types.VARCHAR);
						colsPreenchidas++;
					}

					pstmt.addBatch();
					batchSize++;

					if (batchSize == this.chunkSize) {

						pstmt.executeBatch();
						batchSize = 0;
					}
				}

				if (batchSize > 0) { // necessário, pois o driver do hsqldb
										// lança exceção caso seja chamado
					// executeBatch sem nenhum addBatch antes

					pstmt.executeBatch();
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

		try (Statement stmt = this.conn.createStatement();
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