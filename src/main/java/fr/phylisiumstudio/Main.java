package fr.phylisiumstudio;

import fr.phylisiumstudio.app.inject.App;

public class Main {
    public static void main(String[] args) {
        App app = new App();
        app.LoadData();
        app.StartServer();
    }
}