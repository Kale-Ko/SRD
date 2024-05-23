package io.github.kale_ko.srd;

import io.github.kale_ko.srd.manager.ManagerServer;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManagerMain {
    public static void main(String[] args) throws Exception {
        Logger logger = LogManager.getLogger("SRD-Server");

        InetAddress hostname = InetAddress.getByName("localhost");
        int httpPort = 8080;
        int httpsPort = 8443;

        boolean https = false;
        Path httpsCertificate = null;
        Path httpsPrivateKey = null;

        String generateCertificate = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("--host") || args[i].equalsIgnoreCase("--hostname")) {
                if (i + 1 >= args.length) {
                    logger.error("Host must be provided after --host");
                    System.exit(1);
                }

                try {
                    hostname = InetAddress.getByName(args[i + 1]);
                    i++;
                } catch (UnknownHostException e) {
                    logger.error("Invalid/unknown host provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-p") || args[i].equalsIgnoreCase("--port")) {
                if (i + 1 >= args.length) {
                    logger.error("Port must be provided after --port");
                    System.exit(1);
                }

                try {
                    if (!https) {
                        httpPort = Integer.parseInt(args[i + 1]);
                    } else {
                        httpsPort = Integer.parseInt(args[i + 1]);
                    }
                    i++;
                } catch (NumberFormatException e) {
                    logger.error("Invalid port provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("--httpPort")) {
                if (i + 1 >= args.length) {
                    logger.error("Port must be provided after --httpPort");
                    System.exit(1);
                }

                try {
                    httpPort = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    logger.error("Invalid httpPort provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("--httpsPort")) {
                if (i + 1 >= args.length) {
                    logger.error("Port must be provided after --httpsPort");
                    System.exit(1);
                }

                try {
                    httpsPort = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    logger.error("Invalid httpsPort provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--https")) {
                https = true;
            } else if (args[i].equalsIgnoreCase("-c") || args[i].equalsIgnoreCase("--httpsCertificate")) {
                if (i + 1 >= args.length) {
                    logger.error("Certificate must be provided after --httpsCertificate");
                    System.exit(1);
                }

                try {
                    httpsCertificate = Path.of(args[i + 1]);

                    i++;
                } catch (InvalidPathException e) {
                    logger.error("Invalid httpsCertificate provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-k") || args[i].equalsIgnoreCase("--httpsPrivateKey")) {
                if (i + 1 >= args.length) {
                    logger.error("Private key must be provided after --httpsPrivateKey");
                    System.exit(1);
                }

                try {
                    httpsPrivateKey = Path.of(args[i + 1]);

                    i++;
                } catch (InvalidPathException e) {
                    logger.error("Invalid httpsPrivateKey provided \"{}\"", args[i + 1]);
                    System.exit(1);
                }
            } else if (args[i].equalsIgnoreCase("-g") || args[i].equalsIgnoreCase("--generateCertificate") || args[i].equalsIgnoreCase("--selfSignedCertificate")) {
                if (i + 1 >= args.length) {
                    logger.error("Hostname must be provided after --generateCertificate");
                    System.exit(1);
                }

                System.out.println(Arrays.toString(args));

                generateCertificate = args[i + 1];
                i++;
            } else {
                logger.error("Invalid/unknown parameter provided \"{}\"", args[i]);
                System.exit(1);
            }
        }

        if (generateCertificate != null) {
            if ((httpsCertificate != null && Files.exists(httpsCertificate)) || (httpsPrivateKey != null && Files.exists(httpsPrivateKey))) {
                logger.error("Cannot generate a key when a key already exists");
                System.exit(1);
            }

            SelfSignedCertificate ssc = new SelfSignedCertificate(generateCertificate);

            if (httpsCertificate != null && httpsPrivateKey != null) {
                Files.copy(ssc.certificate().toPath(), httpsCertificate);
                Files.copy(ssc.privateKey().toPath(), httpsPrivateKey);
            } else if (httpsCertificate != null || httpsPrivateKey != null) {
                logger.error("Both certificate and private key must be passed to store generated key");
                System.exit(1);
            } else {
                httpsCertificate = ssc.certificate().toPath();
                httpsPrivateKey = ssc.privateKey().toPath();
            }
        }

        if (https && (httpsCertificate == null || httpsPrivateKey == null)) {
            logger.error("httpsCertificate and httpsPrivateKye must be provided when https is enabled");
            System.exit(1);
        }

        if (!Files.exists(httpsCertificate)) {
            logger.error("The passed httpsCertificate does not exist \"{}\"", httpsCertificate);
            System.exit(1);
        } else if (!Files.exists(httpsPrivateKey)) {
            logger.error("The passed httpsPrivateKey does not exist \"{}\"", httpsPrivateKey);
            System.exit(1);
        }

        ManagerServer.Config managerConfig = new ManagerServer.Config(hostname, httpPort, https, httpsPort, httpsCertificate, httpsPrivateKey);
        ManagerServer managerServer = new ManagerServer(logger, managerConfig);

        managerServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(managerServer::stop));

        Thread.sleep(1000);
    }
}