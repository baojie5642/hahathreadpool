package com.baojie.zk.example.concurrent.filecount;

import java.io.File;

public class CoreFile {

    private final Poison p;
    private final File f;

    private CoreFile(Poison p, File f) {
        this.p = p;
        this.f = f;
    }

    public static CoreFile create(Poison p, File f) {
        return new CoreFile(p, f);
    }

    public Poison getP() {
        return p;
    }

    public File getF() {
        return f;
    }

    @Override
    public String toString() {
        return "CoreFile{" +
                "p=" + p +
                ", f=" + f +
                '}';
    }
}
