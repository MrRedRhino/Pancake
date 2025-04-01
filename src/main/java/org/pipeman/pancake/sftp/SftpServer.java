package org.pipeman.pancake.sftp;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ShellFactory;
import org.apache.sshd.sftp.server.SftpFileSystemAccessor;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

public class SftpServer {
    public static void main(String[] args) throws IOException {
        try (SshServer sshd = SshServer.setUpDefaultServer()) {
            sshd.setPort(4200);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Path.of("ssh-key")));
            sshd.setShellFactory(new ShellFactory() {
                @Override
                public Command createShell(ChannelSession channel) throws IOException {
                    return new Command() {
                        boolean running = true;

                        @Override
                        public void setExitCallback(ExitCallback callback) {

                        }

                        @Override
                        public void setErrorStream(OutputStream err) {

                        }

                        @Override
                        public void setInputStream(InputStream in) {
                            Thread thread = new Thread(() -> {
                                try {
                                    while (running) {
                                        int read = in.read();
                                        System.out.println((char) read + " " + read);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                            thread.setDaemon(true);
                            thread.start();
                            // delete: 127
                            // escape: 27
                            // ctrl + d: 4
                            // ctrl + c: 3
                            // tab: 9
                        }

                        @Override
                        public void setOutputStream(OutputStream out) {

                        }

                        @Override
                        public void start(ChannelSession channel, Environment env) throws IOException {

                        }

                        @Override
                        public void destroy(ChannelSession channel) throws Exception {
                            running = false;
                        }
                    };
                }
            });
            sshd.setPasswordAuthenticator((username, password, session) -> {
                System.out.println(username);
                System.out.println(password);
                return true;
            });

            SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory.Builder()
                    .withFileSystemAccessor(new SftpFileSystemAccessor() {

                    })
                    .build();

            sshd.setSubsystemFactories(List.of(sftpFactory));

            sshd.start();

            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(1000000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
