package fr.phylisiumstudio;

import fr.phylisiumstudio.app.App;

public class Main {
    public static void main(String[] args) {
        App app = new App();
        app.SetupServer();
        app.LoadConfig();
        app.SetupGuice();
        app.LoadData();
        app.StartServer();
    }
}