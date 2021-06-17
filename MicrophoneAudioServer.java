package multicastav;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author evand
 */
public class MicrophoneAudioServer {

    private static final String IP_ADDRESS = "224.0.0.1";
    // Audio recebido dos pacotes para enviar aos speakers do device
    private static AudioInputStream input;
    // formato deve combinar com o que definimos no Sender
    //Para evitar audio distorcido e errado
    private static AudioFormat format;

    //lines usadas para conectar ao speaker e dar play no áudio
    private static DataLine.Info dli;
    private static SourceDataLine sdl;

    //Especificações
    //taxa de sample - mesma coisa do Sender
    private static float rate = 44100.0f;
    private static int channels = 2;
    // tamanho do sample em bits (conversão para sinal digital para envio via rede)
    // representação digital do áudio
    private static int sampleSize = 16;
    // ordem dos bits
    private static boolean bigEndian = false;

    private static void playSoundOnSpeaker(byte soundBytes[]) {
        try {
            System.out.println("<> No Speaker <>");
            //gravar nossos dados para o speaker source
            sdl.write(soundBytes, 0, soundBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void receiveAudio(int port) {
        System.out.println("Cliente iniciado na porta: " + port);
        //Definindo também para usar IPv4
        System.setProperty("java.net.preferIPv4Stack", "true");

        try {

            // Criando o gripo de multicast
            InetAddress group = InetAddress.getByName(IP_ADDRESS);
            MulticastSocket socket = new MulticastSocket(port);
            // Join no grupo
            // todos dentro do grupo podem ouvir o rádio
            socket.joinGroup(group);

            //byte array - vamos receber do pacote
            //mesmo tamanho também é necessário
            byte[] receivedData = new byte[4096];
            //Necessário combinar com os parâmetros enviados no Sender
            format = new AudioFormat(rate, sampleSize, channels, true, bigEndian);

            //Convertendo os dados do áudio usando esse formato
            dli = new DataLine.Info(SourceDataLine.class, format);

            //Enviando aos speakers
            sdl = (SourceDataLine) AudioSystem.getLine(dli);

            //Abrindo a thread para iniciar o processamento
            // Recebe os dados e em outra thread envia ao speaker do dispositivo
            sdl.open(format);
            sdl.start();

            // Receber o pacote dos dados   
            DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);

            // Pegar o pacote e converter devolta à bytes que podemos enviar ao audio decoder      
            ByteArrayInputStream bais = new ByteArrayInputStream(receivedPacket.getData());

            //Tudo setado
            //Processo contínuo        
            while (true) {
                // Recebe dados no socket
                socket.receive(receivedPacket);//não passa daqui até receber um pacote
                // Cria um audioinputstream desse pacote
                input = new AudioInputStream(bais, format, receivedPacket.getLength());
                //chama o método para dar play no som
                playSoundOnSpeaker(receivedPacket.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
