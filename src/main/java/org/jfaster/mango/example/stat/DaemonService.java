package org.jfaster.mango.example.stat;

import org.jfaster.mango.datasource.SimpleDataSourceFactory;
import org.jfaster.mango.example.stat.dao.UserDao;
import org.jfaster.mango.example.stat.pojo.User;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ash
 */
@Service
public class DaemonService implements BeanFactoryPostProcessor {

  ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory clbf) throws BeansException {
    final UserDao userDao = clbf.getBean(UserDao.class);
    SimpleDataSourceFactory sdsf = clbf.getBean(SimpleDataSourceFactory.class);

    // 创建表
    createTable(sdsf);

    // 每隔1秒定时执行DAO方法
    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          User user = createRandomUser();
          int id = userDao.addUser(user);
          userDao.getUserById(id);
          userDao.getUserByName("ash");
          user.setId(id);
          user.setAge(1);
          userDao.updateUser(user);
          userDao.deleteUserById(id);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  private void createTable(SimpleDataSourceFactory sdsf) {
    DataSource dataSource = sdsf.getDataSource();
    try {
      Connection conn = dataSource.getConnection();
      Statement stat = conn.createStatement();
      String sql = "CREATE TABLE user (" +
          "id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1) NOT NULL, " +
          "name VARCHAR(25), " +
          "age INTEGER, " +
          "update_time TIMESTAMP, " +
          "PRIMARY KEY (id));";
      stat.execute(sql);
      stat.close();
      conn.close();
    } catch (SQLException e) {
      throw new RuntimeException("create table error");
    }
  }

  private User createRandomUser() {
    Random r = new Random();
    String name = "lucy";
    int age = r.nextInt(200);
    Date date = new Date();
    User user = new User();
    user.setName(name);
    user.setAge(age);
    user.setUpdateTime(date);
    return user;
  }

}
