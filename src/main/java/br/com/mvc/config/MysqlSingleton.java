package br.com.mvc.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlSingleton {

    private static final String URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:mysql://mysql:3306/sugestfilmes?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Sao_Paulo&useSSL=false&allowPublicKeyRetrieval=true");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "mvc_user");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "local_app_change_me");

    private static MysqlSingleton instance;
    private final ThreadLocal<Connection> conexoes = new ThreadLocal<>();

    private MysqlSingleton() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Driver MySQL nao encontrado no projeto.", e
            );
        }
    }

    public static synchronized MysqlSingleton getInstance() {
        if (instance == null) {
            instance = new MysqlSingleton();
        }
        return instance;
    }

    private Connection obterConexao() throws SQLException {
        Connection conexao = this.conexoes.get();
        if (conexao == null || conexao.isClosed() || !conexao.isValid(2)) {
            conexao = DriverManager.getConnection(URL, USER, PASSWORD);
            this.conexoes.set(conexao);
        }
        return conexao;
    }

    public ResultSet executar(
            String sql,
            Object... parametros
    ) throws SQLException {

        Connection conn = this.obterConexao();

        PreparedStatement ps = conn.prepareStatement(sql);

        for (int i = 0; i < parametros.length; i++) {
            ps.setObject(i + 1, parametros[i]);
        }

        return ps.executeQuery();
    }

    public int executarUpdate(
            String sql,
            Object... parametros
    ) throws SQLException {

        Connection conn = this.obterConexao();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }

            return ps.executeUpdate();
        }
    }
}