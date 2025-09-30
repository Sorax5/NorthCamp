package fr.phylisiumstudio;

import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.IApplication;

public class Main {
    public static void main(String[] args) {
        IApplication app = new App();
        app.OnEnable();
        Runtime.getRuntime().addShutdownHook(new Thread(app::OnDisable));
    }

}