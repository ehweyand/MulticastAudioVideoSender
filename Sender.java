/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package multicastav;

/**
 *
 * @author evand
 */
import com.sun.image.codec.jpeg.ImageFormatException;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author evand
 */
public class Sender {

    // Constantes
    public static int HEADER_SIZE = 8;
    public static int MAX_PACKETS = 255;
    public static int SESSION_START = 128;
    public static int SESSION_END = 64;
    public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;
    public static int MAX_SESSION_NUMBER = 255;
    public static String OUTPUT_FORMAT = "jpg";
    public static int COLOUR_OUTPUT = BufferedImage.TYPE_INT_RGB;
    public static double SCALING = 0.5;
    public static int SLEEP_MILLIS = 2000;
    public static String IP_ADDRESS = "224.0.0.1";
    public static int PORT = 8000;

    //Captura a tela toda e retorna uma imagem
    public static BufferedImage getScreenshot() throws AWTException,
            ImageFormatException, IOException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        Rectangle screenRect = new Rectangle(screenSize);

        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(screenRect);

        return image;
    }

    // Converte uma imagem para array de bytes
    public static byte[] bufferedImageToByteArray(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    //Muda o tamanho de uma imagem
    public static BufferedImage scale(BufferedImage source, int w, int h) {
        Image image = source
                .getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(w, h, COLOUR_OUTPUT);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    //Diminui a imagem
    public static BufferedImage shrink(BufferedImage source, double factor) {
        int w = (int) (source.getWidth() * factor);
        int h = (int) (source.getHeight() * factor);
        return scale(source, w, h);
    }

    //Envia uma imagem via multicast
    private boolean sendImage(byte[] imageData, String multicastAddress, int port) {
        InetAddress ia;

        boolean ret = false;
        int ttl = 2;

        try {
            ia = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return ret;
        }

        MulticastSocket ms = null;

        try {
            ms = new MulticastSocket();
            ms.setTimeToLive(ttl);
            DatagramPacket dp = new DatagramPacket(imageData, imageData.length,
                    ia, port);
            ms.send(dp);
            ret = true;
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (ms != null) {
                ms.close();
            }
        }

        return ret;
    }

    public static void sendVideo() {

        Sender sender = new Sender();
        int sessionNumber = 0;
        boolean multicastImages = false;

        //Inicia o processamento de envio
        try {
            //Tentando mandar v??rias imagens continuamente...
            while (true) {
                BufferedImage image;

                /* Pega a imagem */
                image = getScreenshot();

                /* Altera o tamanho da imagem */
                image = shrink(image, SCALING);
                byte[] imageByteArray = bufferedImageToByteArray(image, OUTPUT_FORMAT);
                int packets = (int) Math.ceil(imageByteArray.length / (float) DATAGRAM_MAX_SIZE);

                /* Se uma imagem tem mais que o M??ximo de pacotes gera um erro */
                if (packets > MAX_PACKETS) {
                    System.out.println("Image is too large to be transmitted!");
                    continue;
                }

                for (int i = 0; i <= packets; i++) {
                    int flags = 0;
                    flags = i == 0 ? flags | SESSION_START : flags;
                    flags = (i + 1) * DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

                    // Verifica se a fatia da imagem vai ter o tamanho aceit??vel para o pacote
                    int size = (flags & SESSION_END) != SESSION_END ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE;

                    /* Configura informa????es adicionais no header udp */
                    byte[] data = new byte[HEADER_SIZE + size];
                    data[0] = (byte) flags; //flags de inicio e fim de sess??o
                    data[1] = (byte) sessionNumber; //sess??o que o pacote pertence
                    data[2] = (byte) packets; //numero de pacotes
                    data[3] = (byte) (DATAGRAM_MAX_SIZE >> 8);
                    data[4] = (byte) DATAGRAM_MAX_SIZE; // tamanho do datagrama
                    data[5] = (byte) i; //qual a fatia que est?? sendo enviada, para organizar no recebimento
                    data[6] = (byte) (size >> 8);
                    data[7] = (byte) size;

                    /* copia a fatia atual para o array */
                    System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);
                    /* envia o pacote multicast*/
                    sender.sendImage(data, IP_ADDRESS, PORT);

                    /* Sai do loop quando o ??ltimo pacote ?? enviado */
                    if ((flags & SESSION_END) == SESSION_END) {
                        break;
                    }
                }
                /* Tempo de f??lego para deixar a reprodu????o mais pausada e mais lenta */
                Thread.sleep(SLEEP_MILLIS);

                /* Aumenta o n??mero de sess??es */
                sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        //Criando um frame para disparar o envio e dar um feedback visual
        JFrame frame = new JFrame("Envio de imagem e ??udio por multicast");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        frame.setVisible(true);
        label.setText("Multicasteando imagem com v??deo ...");
        frame.pack();

        Runnable task1 = () -> {
            sendVideo();
        };

        Runnable task2 = () -> {
            MicrophoneAudioClient.sendMicrophoneAudio(9000);
        };

        new Thread(task1).start();
        new Thread(task2).start();

    }
}
