module com.example.csc311_db_ui_semesterlongproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires org.apache.derby.engine;
    requires org.apache.derby.commons;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;

    opens viewmodel;
    exports viewmodel;
    opens dao;
    exports dao;
    opens model;
    exports model;
    opens service;
    exports service;
}
