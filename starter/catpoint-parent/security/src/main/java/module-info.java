module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires miglayout;
    requires java.desktop;
    requires java.prefs;
    requires java.sql;
    requires com.google.common;
    requires com.google.gson;
    opens com.udacity.catpoint.security.data to com.google.gson;
}