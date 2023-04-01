module il.ac.kinneret.mjmay.hls.hlsjava {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires java.logging;

    opens il.ac.kinneret.mjmay.hls.hlsjava to javafx.fxml;
    opens il.ac.kinneret.mjmay.hls.hlsjava.model to javafx.base;
    exports il.ac.kinneret.mjmay.hls.hlsjava;
}