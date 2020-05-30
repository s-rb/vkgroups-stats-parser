package ru.list.surkovr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


// TODO ввод групп через форму на главной странице
// После логина редирект на главную (по умолчанию вывод - today)
// Выбор периодов ссылками (кнопками)
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
