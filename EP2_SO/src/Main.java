import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

//classe responsavel por amazenar a base de dados em memoria
class BasedeDados{
    String[] base;
    int tamanhoBase;
    Random indicealeatorio = new Random();
    public BasedeDados(int tamanhoBase){
        base = new String[tamanhoBase];
        this.tamanhoBase = tamanhoBase;
    }

    void leArquivo(File arquivo){
        int i = 0;
        try {
            Scanner leitor = new Scanner(arquivo);
            while (leitor.hasNextLine()){
                String dado = leitor.nextLine();
                base[i] = dado;
                i++;
            }
        }
        catch (FileNotFoundException e) {
        }
    }

    //devolve uma palavra aleatoria para um leitor
    String devolvePalavraaleatoria(){
        return base[indicealeatorio.nextInt(tamanhoBase)];
    }

    //modifica uma palavra aleatoria para qualquer outra proposta por um escritor
    void alteraPalavraaleatoria(String novapalavra){
        base [indicealeatorio.nextInt(tamanhoBase)] = novapalavra;
    }

    //imprime a base no estado atual (com mofificacoes)
    void imprimeBase(){
        for(int i = 0; i < tamanhoBase; i++){
            System.out.println(base[i]);
        }
    }
}

//Thread que escreve na base e a trava para leitores e escritores
class ThreadEscritora extends Thread {
    Semaphore db;
    BasedeDados basededados;
    String palavra = "MODIFICADO";

    public ThreadEscritora(Semaphore db, BasedeDados basededados){
        this.db = db;
        this.basededados = basededados;
    }
    public void run() {
        try {
            db.acquire();
            for(int i = 0; i < 100; i++) {
                basededados.alteraPalavraaleatoria(palavra);
            }
            Thread.sleep(1);
            db.release();
        }
        catch (InterruptedException e){
        }
    }

}

//Thread que le a base e a trava para escritores
class ThreadLeitora extends Thread {
    Semaphore mutex;
    Semaphore db;
    ContadorLeitores rc;
    BasedeDados basededados;
    String palavra;
    int numeroLeitores;
    public ThreadLeitora(Semaphore mutex, Semaphore db, ContadorLeitores rc, BasedeDados basededados){
        this.mutex = mutex;
        this.db = db;
        this.rc = rc;
        this.basededados = basededados;
    }
    public void run() {
        try {
            mutex.acquire();
            rc.adicionaLeitor();
            numeroLeitores = rc.devolveLeitores();
            if (numeroLeitores == 1) db.acquire();
            mutex.release();
            for(int i = 0; i < 100; i++) {
                palavra = basededados.devolvePalavraaleatoria();
            }
            Thread.sleep(1);
            mutex.acquire();
            rc.removeLeitor();
            numeroLeitores = rc.devolveLeitores();
            if (numeroLeitores == 0) db.release();
            mutex.release();
        }
        catch (InterruptedException e) {
        }

    }

}

//Thread que le a base e a trava para leitores e escritores
class ThreadLeitoratrava extends Thread {
    Semaphore db;
    BasedeDados basededados;
    String palavra = "MODIFICADO";

    public ThreadLeitoratrava(Semaphore db, BasedeDados basededados){
        this.db = db;
        this.basededados = basededados;
    }

    /*Como a ideia dessa thread é ler algo na base e travar a
     mesma tanto para leitores como escritores,
     sua implementacao é basicamente a mesma de uma thread escritora comum,
      mudando apenas o tipo de operacao a ser realizada na base*/
    public void run() {
        try {
            db.acquire();
            for(int i = 0; i < 100; i++) {
                palavra = basededados.devolvePalavraaleatoria();
            }
            Thread.sleep(1);
            db.release();
        }
        catch (InterruptedException e){
        }
    }

}

//classe que armazena uma lista de numeros aleatorios sem repeticao
class NumerosRandom {
    private Integer[] numeros;
    private int prox;
    private int tam;

    public NumerosRandom(int tamanho){
        numeros = new Integer[tamanho];
        prox = 0;
        tam = tamanho;
    }

    public void geranumeros() {
        Random radom  = new Random();
        int i;
        for (i = 0; i < tam; i++){
            int numeroTmp = radom.nextInt(tam);

            boolean existe = Arrays.asList(numeros).contains(numeroTmp);

            if(existe){
                i = i - 1;
            }
            else{
                numeros[i] = numeroTmp;
            }
        }
    }

    public int devolvenum(){
        int atual = prox;
        prox ++;
        return numeros[atual];
    }
}

//classe que atua como o rc para os leitores
class ContadorLeitores{
    int Leitores;
    public ContadorLeitores(int numleitores){
        Leitores = numleitores;
    }

    void adicionaLeitor(){
        Leitores = Leitores + 1;
    }
    void removeLeitor(){
        Leitores = Leitores - 1;
    }
    int devolveLeitores(){
        return Leitores;
    }
}
public class Main {
    public static void main(String args[]) {
        //Semaforo
        Semaphore mutex = new Semaphore(1);
        Semaphore db = new Semaphore(1);
        ContadorLeitores rc = new ContadorLeitores(0);

        //Ler arquivo
        File arquivo = new File("bd.txt");
        BasedeDados bd = new BasedeDados(36242);
        bd.leArquivo(arquivo);

        //Array de threads para o número maximo que teremos no sistema, no caso 100
        Thread[] threads = new Thread[100];
        int i, j, k, l;
        //As variaveis abaixo nos ajudarao a marcar a media do tempo de execucao
        long inicio, fim, total, media;

        System.out.println("Usando Leitores/Escritores");
        /*A cada i temos uma proporcao diferente.
        Ex: Leitores 0 X Escritores 100
            Leitores 1 X Escritores 99
            [...]
            Leitores 99 X Escritores 1
            Leitores 100 X Escritores 0
         */
        for (i = 0; i < 101; i++) {
            //No i-esimo temos um tempo total de execucao inicial de 0 ms
            total = 0;
            for(l = 0; l < 50; l++) {
                NumerosRandom n = new NumerosRandom(100);
                n.geranumeros();
                for (j = 0; j < i; j++) {
                    threads[n.devolvenum()] = new ThreadLeitora(mutex, db, rc, bd);
                }
                for (j = 0; j < (100 - i); j++) {
                    threads[n.devolvenum()] = new ThreadEscritora(db, bd);
                }
                //registrando o inicio da execucao das threads
                inicio = System.currentTimeMillis();
                //iniciamos todas as threads
                for (k = 0; k < 100; k++) {
                    threads[k].start();
                }
                //usamos join para que a thread principal espere as outras terminarem
                for (k = 0; k < 100; k++){
                    try {
                        threads[k].join();
                    } catch (InterruptedException e) {
                    }
                }
                //regitrando o fim da execucao de todas as threads, menos a principal
                fim = System.currentTimeMillis();
                total = total + (fim - inicio);
            }
            //media das 50 execucoes
            media = total/50;
            System.out.printf("Media de 50 exec caso: Leitores %d | Escritores %d -> ", i, (100 - i));
            System.out.println(media);
        }

        System.out.println("");

        System.out.println("Nao Usando Leitores/Escritores");
        //a mesma coisa do "for" anterior, mas sem usar Readers e Writers; ou seja, cada thread (leitora ou escritora) trava a base
        for (i = 0; i < 101; i++) {
            total = 0;
            for(l = 0; l < 50; l++) {
                NumerosRandom n = new NumerosRandom(100);
                n.geranumeros();
                for (j = 0; j < i; j++) {
                    threads[n.devolvenum()] = new ThreadLeitoratrava(db, bd);
                }
                for (j = 0; j < (100 - i); j++) {
                    threads[n.devolvenum()] = new ThreadEscritora(db, bd);
                }
                inicio = System.currentTimeMillis();
                for (k = 0; k < 100; k++) {
                    threads[k].start();
                }
                for (k = 0; k < 100; k++){
                    try {
                        threads[k].join();
                    } catch (InterruptedException e) {
                    }
                }
                fim = System.currentTimeMillis();
                total = total + (fim - inicio);
            }
            media = total/50;
            System.out.printf("Media de 50 exec caso: Leitores %d | Escritores %d -> ", i, (100 - i));
            System.out.println(media);
        }
    }
}

