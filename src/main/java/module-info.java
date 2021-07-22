module JavaTop {

    requires java.logging;
    requires java.management.rmi;
    requires java.rmi;
    requires jdk.internal.jvmstat;
    requires jdk.management.agent;

    requires transitive java.desktop;
    requires transitive java.management;
	requires jdk.attach;
}