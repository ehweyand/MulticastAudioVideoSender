package multicastav;


/*
 * Recursos: UDP e Multicasting
 * Converte em um segmento de áudio e converte para um pacote que é enviado pela rede
 */

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class MicrophoneAudioClient {

    private static final String IP_ADDRESS = "224.0.0.1";
    
    public static void sendMicrophoneAudio(int port) {
        // Define que usará ipv4
        System.setProperty("java.net.preferIPv4Stack", "true");
        //DataLine para pegar os dados do microfone
        TargetDataLine tdl;
        DatagramPacket packet;

        //Conversão e salvar em formato digital o áudio
        // Define o encoding
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        // sample do audio em herz
        float rate = 44100.0f;
        // numero de canais de áudio
        int channels = 2;
        // tamanho do sample em bits (conversão para sinal digital para envio via rede)
        // representação digital do áudio
        int sampleSize = 16;
        // ordem dos bits
        boolean bigEndian = false;

        // endereço ip
        InetAddress address;

        System.out.println("Servidor iniciado. Porta: " + port);

        // objeto de formato de áudio
        // forma como java salva a carga de áudio, digitalmente para envio na rede
        // frame rate do áudio (sampleSize / 8) * channels
        AudioFormat format = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8) * channels, rate, bigEndian);

        // Conexão com o microfone
        // pega as informações e converte para um array de bytes para enviar na rede
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // verifica se o dataline tem suporte
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Data line não tem suporte");
        }

        // tratamento das excessões
        try {
            tdl = (TargetDataLine) AudioSystem.getLine(info);
            //Abre a line e inicia a thread no background, que está fazendo o sampling do mic
            // Salvando no array de byte
            tdl.open(format);
            tdl.start();
            byte[] data = new byte[4096];

            //Converter em um pacote para enviar agora os dados do mic!
            address = InetAddress.getByName(IP_ADDRESS);
            //socket multicast
            MulticastSocket socket = new MulticastSocket();

            //continua enviando continuamente os pacotes com os dados de áudio
            while (true) {
                //le os dados da line, salva no array data.
                tdl.read(data, 0, data.length);
                //cria um pacote
                packet = new DatagramPacket(data, data.length, address, port);
                //enviar
                socket.send(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
