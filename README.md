# Leitores-e-Escritores
Aqui apresentamos uma possível solução para o problema dos leitores e escritores usando threads. Note que damos prioridade aos leitores, ou seja, um escritor só acessa a região crítica se não houverem leitores nela (sendo que vários leitores podem chegar enquanto um escritor espera, aumentando a espera do mesmo)
