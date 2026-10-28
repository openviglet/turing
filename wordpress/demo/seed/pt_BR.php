<?php
/**
 * The demo storefront in Portuguese (D217), for a site whose language is pt_BR: catalog.php and
 * journal.php seed this copy in place of their English, keyed by the English it replaces, so
 * one seed writes whichever language the site speaks. Written for Brazilian shoppers: numbers
 * in Brazilian notation, units and sizes as they are.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- seed data read by WP-CLI, not plugin code.

return array(
	'categories' => array(
		'waterproof-jackets' => 'Jaquetas impermeáveis',
		'insulated-jackets'  => 'Jaquetas térmicas',
		'fleece-midlayers'   => 'Fleeces e segundas camadas',
		'hiking-boots'       => 'Botas de trilha',
		'backpacks'          => 'Mochilas',
		'tents'              => 'Barracas',
		'sleeping-bags'      => 'Sacos de dormir',
		'accessories'        => 'Acessórios',
	),

	// Attribute values, as the facets and the product pages show them.
	'terms'      => array(
		'Slate'                => 'Ardósia',
		'Moss'                 => 'Musgo',
		'Ember'                => 'Brasa',
		'Sky'                  => 'Céu',
		'Glacier'              => 'Geleira',
		'Navy'                 => 'Marinho',
		'Sand'                 => 'Areia',
		'Black'                => 'Preto',
		'Brown'                => 'Marrom',
		'Nylon'                => 'Náilon',
		'Gore-Tex'             => 'Gore-Tex',
		'Polyester'            => 'Poliéster',
		'Down'                 => 'Pluma',
		'Synthetic insulation' => 'Enchimento sintético',
		'Merino wool'          => 'Lã merino',
		'Leather'              => 'Couro',
		'Synthetic'            => 'Sintético',
		'Suede'                => 'Camurça',
		'Silk'                 => 'Seda',
		'Aluminium'            => 'Alumínio',
		'Polycarbonate'        => 'Policarbonato',
		'Steel'                => 'Aço',
		'Polyurethane'         => 'Poliuretano',
		'Hiking'               => 'Trilha',
		'Travel'               => 'Viagem',
		'Mountaineering'       => 'Montanhismo',
		'Running'              => 'Corrida',
		'Camping'              => 'Acampamento',
		'1 person'             => '1 pessoa',
		'2 person'             => '2 pessoas',
		'4 person'             => '4 pessoas',
		'Regular'              => 'Padrão',
		'Long'                 => 'Longo',
		'One size'             => 'Tamanho único',
	),

	// The pages demo/setup.php makes, and the site's tagline (D223).
	'pages'      => array(
		'Home'                     => 'Início',
		'Search the shop'          => 'Buscar na loja',
		'Trail journal'            => 'Diário de trilha',
		'Compare searches'         => 'Comparar buscas',
		'Gear for the high ground' => 'Equipamento para as alturas',
	),

	// The words around a description's facts, and the facet labels.
	'labels'     => array(
		'Colour'    => 'Cor',
		'Materials' => 'Materiais',
		'Care'      => 'Cuidados',
		'Category'  => 'Categoria',
		'Stock'     => 'Estoque',
		'Material'  => 'Material',
		'Use'       => 'Uso',
	),

	'care'       => array(
		'waterproof-jackets' => 'Lave a 30 °C com um limpador técnico e seque na secadora em temperatura baixa para reativar o acabamento repelente à água.',
		'insulated-jackets'  => 'Lave a 30 °C com sabão próprio para pluma ou enchimento sintético e seque na secadora em temperatura baixa com duas bolinhas de tênis limpas.',
		'fleece-midlayers'   => 'Lave na máquina a 30 °C. Não passe a ferro.',
		'hiking-boots'       => 'Escove a lama, seque longe de fontes de calor e reimpermeabilize com um tratamento para couro ou tecido.',
		'backpacks'          => 'Limpe com um pano úmido. Não lave na máquina.',
		'tents'              => 'Monte para secar antes de guardar, dobrada sem apertar e longe do sol.',
		'sleeping-bags'      => 'Guarde solto no saco de armazenamento, sem comprimir. Lave raramente, com sabão especializado.',
		'accessories'        => 'Enxágue e seque depois do uso; veja a etiqueta para lavar.',
	),

	// Each model by its English name: its name, the lead and the facts.
	'models'     => array(
		'Catbells light rain jacket'       => array(
			'Jaqueta de chuva leve Catbells',
			'Uma jaqueta de chuva leve que se guarda no próprio bolso, para um dia de trilha com previsão de pancadas.',
			'Peso 240 g (tamanho M). Coluna d’água de 15.000 mm; respirabilidade de 20.000 g/m²/24h. Costuras seladas, capuz que se enrola na gola e dois bolsos laterais.',
		),
		'Helvellyn mountain shell'         => array(
			'Jaqueta de montanha Helvellyn',
			'Uma jaqueta impermeável de três camadas para cristas com chuva e vento, com capuz que cabe sobre o capacete.',
			'Peso 420 g (tamanho M). Coluna d’água de 28.000 mm. Zíperes de ventilação nas axilas, aba estruturada no capuz e bolsos acima da cadeirinha e da barrigueira.',
		),
		'Borrowdale everyday waterproof'   => array(
			'Jaqueta impermeável do dia a dia Borrowdale',
			'Uma jaqueta impermeável para a cidade e caminhadas leves, comprida o bastante para cobrir o quadril.',
			'Peso 380 g (tamanho M). Coluna d’água de 10.000 mm. Forro macio por dentro, zíper de dois cursores e bolso interno para o celular.',
		),
		'Wasdale running shell'            => array(
			'Jaqueta de corrida Wasdale',
			'Uma jaqueta impermeável mínima para corrida de montanha, com costuras seladas que atendem às regras de equipamento das provas.',
			'Peso 150 g (tamanho M). Coluna d’água de 20.000 mm. Punhos elásticos, capuz ajustado e ventilação nas costas.',
		),
		'Skiddaw waterproof trousers'      => array(
			'Calça impermeável Skiddaw',
			'Uma sobrecalça impermeável com zíperes laterais de ponta a ponta, para vestir por cima da bota quando a chuva aperta.',
			'Peso 290 g (tamanho M). Coluna d’água de 15.000 mm. Tornozelos reforçados contra o atrito dos grampões.',
		),
		'Blencathra down jacket'           => array(
			'Jaqueta de pluma Blencathra',
			'Uma jaqueta de pluma quente para paradas no frio e acampamentos de inverno, com pluma de ganso 800 de origem responsável.',
			'Peso 480 g (tamanho M). 180 g de pluma. Capuz que cabe sobre o capacete. Aquece até cerca de -10 °C com pouco movimento.',
		),
		'Glaramara synthetic jacket'       => array(
			'Jaqueta sintética Glaramara',
			'Uma jaqueta com enchimento sintético que continua aquecendo úmida, para o frio molhado do inverno britânico.',
			'Peso 420 g (tamanho M). Enchimento sintético de 100 g/m² que mantém o volume na garoa. Guarda-se no bolso do peito.',
		),
		'Haystacks down vest'              => array(
			'Colete de pluma Haystacks',
			'Um colete de pluma para manter o tronco aquecido por baixo da jaqueta, com os braços livres para escalaminhar.',
			'Peso 210 g (tamanho M). Pluma de pato 700. Bolsos laterais com zíper.',
		),
		'Kentmere hybrid jacket'           => array(
			'Jaqueta híbrida Kentmere',
			'Isolamento na frente e fleece elástico nas costas e nas laterais, para esforço constante em dias frios.',
			'Peso 330 g (tamanho M). Respira na subida e aquece no cume.',
		),
		'Bowfell parka'                    => array(
			'Parka Bowfell',
			'Uma parka de pluma longa, até as coxas, para acampamentos de inverno e noites paradas.',
			'Peso 890 g (tamanho M). 300 g de pluma 750. Aquece até cerca de -20 °C em repouso.',
		),
		'Fairfield grid fleece'            => array(
			'Fleece grid Fairfield',
			'Um fleece com trama grid que segura o calor e deixa o suor sair, a segunda camada para a maioria dos dias na montanha.',
			'Peso 280 g (tamanho M). Meio zíper e passadores para o polegar. Seca rápido.',
		),
		'Coniston merino base layer'       => array(
			'Primeira camada de merino Coniston',
			'Uma primeira camada de lã merino que continua fresca por dias, macia o bastante para pele sensível.',
			'Peso 190 g (tamanho M). Merino de 200 g/m². Costuras planas que não esfolam sob a mochila.',
		),
		'Langdale heavyweight fleece'      => array(
			'Fleece grosso Langdale',
			'Uma jaqueta de fleece grossa e quente para as noites no acampamento e as manhãs frias na barraca.',
			'Peso 520 g (tamanho M). Zíper inteiro, gola alta e dois bolsos com zíper.',
		),
		'Wansfell running top'             => array(
			'Camiseta de corrida Wansfell',
			'Uma camiseta leve de manga longa para correr no tempo fresco.',
			'Peso 140 g (tamanho M). Afasta o suor e seca em uma hora no varal.',
		),
		'Grisedale wool jumper'            => array(
			'Suéter de lã Grisedale',
			'Um suéter de tricô de lã que aquece mesmo molhado, tanto para o bar depois da trilha quanto para a trilha.',
			'Peso 460 g (tamanho M). Tricotado com merino sem tingimento.',
		),
		'Scafell waterproof hiking boot'   => array(
			'Bota de trilha impermeável Scafell',
			'Uma bota de couro impermeável para terreno acidentado e molhado, firme o bastante para um dia inteiro em pedras soltas.',
			'Peso 1.380 g o par (tamanho 42). Membrana Gore-Tex, solado Vibram e proteção de borracha. Aceita grampão C1.',
		),
		'Pillar lightweight hiking boot'   => array(
			'Bota de trilha leve Pillar',
			'Uma bota impermeável leve para trilhas bem marcadas, confortável desde a primeira caminhada.',
			'Peso 980 g o par (tamanho 42). Membrana impermeável e entressola amortecida.',
		),
		'Ennerdale trail shoe'             => array(
			'Tênis de trilha Ennerdale',
			'Um tênis de trail running com cravos profundos para lama e grama molhada.',
			'Peso 620 g o par (tamanho 42). Cravos de 5 mm, placa de proteção contra pedras e drop de 6 mm. Não é impermeável: escorre a água e seca.',
		),
		'Kirkstone winter boot'            => array(
			'Bota de inverno Kirkstone',
			'Uma bota de montanhismo de inverno com isolamento, para neve e gelo, compatível com grampões B2.',
			'Peso 1.780 g o par (tamanho 42). Isolada até cerca de -15 °C. Solado rígido e apoio no calcanhar.',
		),
		'Mardale approach shoe'            => array(
			'Tênis de aproximação Mardale',
			'Um tênis de aproximação aderente para escalaminhadas e acessos pedregosos.',
			'Peso 760 g o par (tamanho 42). Borracha aderente na ponta para pisar em bordas.',
		),
		'Helvellyn 28 litre daypack'       => array(
			'Mochila de ataque Helvellyn 28 litros',
			'Uma mochila de ataque de 28 litros com costas ventiladas, do tamanho certo para um dia inteiro na montanha.',
			'Peso 980 g. Capa para mochila no fundo, compartimento para reservatório de água e bolsos na barrigueira.',
		),
		'Scafell 45 litre pack'            => array(
			'Mochila Scafell 45 litros',
			'Uma mochila de 45 litros para pernoites em refúgio e uma noite fora com equipamento leve.',
			'Peso 1.450 g. Costas com altura ajustável e tampa removível.',
		),
		'Ennerdale 65 litre trekking pack' => array(
			'Mochila cargueira Ennerdale 65 litros',
			'Uma mochila cargueira de 65 litros para travessias de vários dias com barraca e comida.',
			'Peso 2.100 g. Armação com ajuste de carga, compartimento para saco de dormir e barrigueira que leva a maior parte do peso.',
		),
		'Wasdale 12 litre running vest'    => array(
			'Colete de hidratação Wasdale 12 litros',
			'Um colete de corrida de 12 litros que leva uma jaqueta impermeável e duas garrafas flexíveis sem balançar.',
			'Peso 310 g. Bolsos frontais para garrafas e celular.',
		),
		'Borrowdale travel pack'           => array(
			'Mochila de viagem Borrowdale',
			'Uma mochila de viagem de 35 litros que abre como mala e cabe nos limites da bagagem de mão.',
			'Peso 1.200 g. Compartimento para notebook e alças que se guardam com zíper.',
		),
		'Catbells 20 litre kids pack'      => array(
			'Mochila infantil Catbells 20 litros',
			'Uma mochila de 20 litros feita para crianças a partir de uns sete anos, com apito na alça peitoral.',
			'Peso 520 g. Detalhes refletivos e etiqueta para o nome por dentro.',
		),
		'Haystacks one-person tent'        => array(
			'Barraca individual Haystacks',
			'Uma barraca de mochileiro para uma pessoa que se monta pelo sobreteto primeiro, para o quarto ficar seco na chuva.',
			'Peso 1.100 g. Coluna d’água de 3.000 mm no sobreteto e 5.000 mm no piso. Uma vareta.',
		),
		'Glaramara two-person tent'        => array(
			'Barraca para duas pessoas Glaramara',
			'Uma barraca para duas pessoas com duas portas e dois avancês, leve o bastante para carregar por uma semana.',
			'Peso 1.750 g. Coluna d’água de 3.000 mm no sobreteto. Monta em cerca de cinco minutos.',
		),
		'Blencathra mountain tent'         => array(
			'Barraca de montanha Blencathra',
			'Uma barraca geodésica de montanha para lugares expostos, com vento, e neve de inverno.',
			'Peso 3.200 g. Quatro varetas cruzadas e saias para neve.',
		),
		'Langdale family tent'             => array(
			'Barraca familiar Langdale',
			'Uma barraca túnel para quatro pessoas com dois quartos e espaço para ficar de pé na sala.',
			'Peso 9.800 g. Coluna d’água de 4.000 mm.',
		),
		'Coniston bivvy bag'               => array(
			'Bivaque Coniston',
			'Um saco de bivaque impermeável e respirável para uma noite sob as estrelas sem barraca.',
			'Peso 480 g. Arco na cabeceira que afasta o tecido do rosto.',
		),
		'Skiddaw three-season down bag'    => array(
			'Saco de dormir de pluma três estações Skiddaw',
			'Um saco de dormir de pluma para três estações, para acampar da primavera ao outono.',
			'Conforto -2 °C, limite -8 °C (EN ISO 23537). Peso 980 g. 600 g de pluma 650.',
		),
		'Bowfell winter down bag'          => array(
			'Saco de dormir de pluma de inverno Bowfell',
			'Um saco de dormir de pluma de inverno para acampamentos na neve.',
			'Conforto -10 °C, limite -17 °C (EN ISO 23537). Peso 1.650 g.',
		),
		'Fairfield synthetic summer bag'   => array(
			'Saco de dormir sintético de verão Fairfield',
			'Um saco de dormir sintético leve para noites de verão e albergues, que aquece mesmo úmido.',
			'Conforto 8 °C, limite 3 °C (EN ISO 23537). Peso 750 g.',
		),
		'Grisedale sleeping mat'           => array(
			'Isolante inflável Grisedale',
			'Um isolante inflável com isolamento térmico, de 6 cm de espessura, para chão frio.',
			'Valor R 4,2. Peso 540 g. Fica do tamanho de uma garrafa de água quando guardado.',
		),
		'Kirkstone silk liner'             => array(
			'Liner de seda Kirkstone',
			'Um liner de seda para saco de dormir que aquece mais e mantém o saco limpo.',
			'Peso 130 g. Acrescenta cerca de 2 °C.',
		),
		'Helvellyn waterproof gloves'      => array(
			'Luvas impermeáveis Helvellyn',
			'Luvas impermeáveis e térmicas para dias frios e molhados no alto da montanha.',
			'Peso 120 g o par (tamanho M). Forro impermeável e palma aderente.',
		),
		'Catbells merino beanie'           => array(
			'Gorro de merino Catbells',
			'Um gorro de merino, quente e fino o bastante para usar por baixo do capuz.',
			'Peso 45 g.',
		),
		'Scafell trekking poles'           => array(
			'Bastões de caminhada Scafell',
			'Um par de bastões de caminhada de alumínio ajustáveis que aliviam os joelhos na descida.',
			'Peso 520 g o par. Ajuste de 105 a 135 cm. Travas rápidas e empunhadura de cortiça.',
		),
		'Wasdale head torch'               => array(
			'Lanterna de cabeça Wasdale',
			'Uma lanterna de cabeça recarregável de 400 lúmens, para as tardes de inverno que terminam no escuro.',
			'Peso 85 g. Até 40 horas no modo baixo. Carrega por USB-C. Resistente à água IPX4.',
		),
		'Borrowdale gaiters'               => array(
			'Polainas Borrowdale',
			'Polainas impermeáveis que impedem a água do brejo e a neve de entrarem na bota.',
			'Peso 240 g o par. Zíper frontal e tira resistente sob o peito do pé.',
		),
		'Ennerdale insulated bottle'       => array(
			'Garrafa térmica Ennerdale',
			'Uma garrafa térmica de aço de 750 ml que mantém o chá quente por doze horas.',
			'Peso 380 g. Quente por 12 horas, gelada por 24.',
		),
		'Kentmere map case'                => array(
			'Porta-mapa Kentmere',
			'Um porta-mapa impermeável que leva um mapa dobrado e fica pendurado no pescoço.',
			'Peso 70 g. Fechamento enrolável.',
		),
		'Haystacks dry bag set'            => array(
			'Kit de sacos estanques Haystacks',
			'Três sacos estanques de fechamento enrolável que mantêm o saco de dormir e a roupa reserva secos dentro da mochila.',
			'Peso 110 g os três. Costuras seladas.',
		),
		'Fairfield camp stove'             => array(
			'Fogareiro Fairfield',
			'Um fogareiro a gás leve que ferve um litro de água em cerca de quatro minutos.',
			'Peso 95 g. Rosqueia em um cartucho de gás padrão. Acendimento piezoelétrico.',
		),
		'Langdale cook set'                => array(
			'Kit de panelas Langdale',
			'Um kit de duas panelas de alumínio que se encaixam em volta do cartucho de gás e do fogareiro.',
			'Peso 320 g. Uma panela de 1,2 litro e outra de 0,8 litro, e uma tampa que vira frigideira.',
		),
	),

	// Each guide by its slug: title, category and paragraphs.
	'journal'    => array(
		'how-to-choose-a-waterproof-jacket'  => array( 'Como escolher uma jaqueta impermeável', 'Guias de compra', array(
			'A coluna d’água diz quanta água um tecido segura; a respirabilidade diz quanto suor ele deixa sair. Para um dia de pancadas, 10.000 mm bastam. Para uma crista com chuva e vento, procure 20.000 mm ou mais e um capuz que caiba sobre o capacete.',
			'A Jaqueta de chuva leve Catbells se guarda no próprio bolso e pesa 240 g: é a que se leva quando a chuva é só previsão. A Jaqueta de montanha Helvellyn é a que se veste quando a chuva já começou, com zíperes de ventilação para a subida. Para a cidade e trilhas baixas, a Jaqueta impermeável do dia a dia Borrowdale é mais comprida e mais macia por dentro.',
			'Seja qual for a escolha, lave a jaqueta: suja, ela encharca. Uma lavagem a 30 °C e uma secadora em temperatura baixa trazem de volta o acabamento repelente à água.',
		) ),
		'down-or-synthetic-insulation'       => array( 'Pluma ou sintético: como escolher uma jaqueta térmica', 'Guias de compra', array(
			'A pluma é a mais quente para o seu peso e a que ocupa menos espaço, mas perde o volume quando molha. O enchimento sintético pesa mais para o mesmo calor e continua funcionando na garoa.',
			'Para dias frios e secos e acampamentos de inverno, a Jaqueta de pluma Blencathra é a mais quente que fazemos para o seu peso. Para o frio molhado do inverno britânico, a Jaqueta sintética Glaramara é a escolha mais segura. Se você se move rápido, a Jaqueta híbrida Kentmere põe isolamento onde o vento bate e fleece elástico onde você transpira.',
		) ),
		'choosing-hiking-boots'              => array( 'Bota de trilha ou tênis de trilha?', 'Guias de compra', array(
			'A bota protege o tornozelo em terreno acidentado e mantém o pé seco no brejo; o tênis é mais leve e seca mais rápido quando molha.',
			'Para pedras soltas, neve e dias longos, a Bota de trilha impermeável Scafell aceita grampão C1. Para trilhas bem marcadas, a Bota de trilha leve Pillar é confortável desde a primeira caminhada. Se você prefere escorrer a ficar seco, o Tênis de trilha Ennerdale tem cravos de 5 mm para a lama. No inverno, só a Bota de inverno Kirkstone aceita grampão B2.',
			'Experimente a bota à tarde, quando o pé está maior, com as meias que você vai usar na trilha.',
		) ),
		'what-size-backpack'                 => array( 'De que tamanho deve ser sua mochila?', 'Guias de compra', array(
			'Um dia na montanha pede espaço para uma jaqueta impermeável, uma camada quente, almoço, água e um kit de primeiros socorros: cerca de 25 a 30 litros. Uma noite em refúgio, 40 a 45. Levando barraca e uma semana de comida, 60 ou mais.',
			'A Mochila de ataque Helvellyn 28 litros guarda uma capa para mochila no fundo. A Mochila Scafell 45 litros leva equipamento leve para uma noite fora. A Mochila cargueira Ennerdale 65 litros carrega uma barraca, com uma barrigueira que leva a maior parte do peso. Para correr, o Colete de hidratação Wasdale 12 litros leva uma jaqueta impermeável e duas garrafas.',
		) ),
		'choosing-a-backpacking-tent'        => array( 'Como escolher uma barraca para mochilão', 'Guias de compra', array(
			'Peso, espaço e resistência ao vento são as três coisas a equilibrar. Uma barraca que se monta pelo sobreteto primeiro mantém o quarto seco quando você monta na chuva.',
			'Sozinho, a Barraca individual Haystacks pesa 1,1 kg e se monta pelo sobreteto primeiro. Em dois, a Barraca para duas pessoas Glaramara dá a cada um uma porta e um avancê. Para lugares expostos, com vento e neve, a Barraca de montanha Blencathra tem quatro varetas cruzadas. Com crianças, a Barraca familiar Langdale tem dois quartos.',
		) ),
		'sleeping-bag-temperature-ratings'   => array( 'Como ler a temperatura de um saco de dormir', 'Guias de compra', array(
			'A norma EN ISO 23537 dá dois números. Conforto é a menor temperatura em que quem sente frio dorme bem; limite é onde quem sente calor dorme encolhido, mas em segurança. Escolha pelo conforto.',
			'O Saco de dormir de pluma três estações Skiddaw tem conforto de -2 °C, o bastante da primavera ao outono. Para acampar na neve, o Saco de dormir de pluma de inverno Bowfell é confortável até -10 °C. Para o verão e albergues, o Saco de dormir sintético de verão Fairfield. Um Isolante inflável Grisedale importa tanto quanto o saco: o frio vem do chão.',
		) ),
		'layering-for-the-hills'             => array( 'Sistema de camadas para a montanha', 'Guias de compra', array(
			'Três camadas fazem quase todo o trabalho: uma primeira camada que afasta o suor, uma segunda que guarda o calor e uma jaqueta que barra vento e chuva.',
			'Comece com a Primeira camada de merino Coniston, que continua fresca por dias. Acrescente o Fleece grid Fairfield, a segunda camada para a maioria dos dias. Leve uma jaqueta como a Jaqueta de chuva leve Catbells e guarde um Colete de pluma Haystacks na mochila para o cume.',
		) ),
		'trail-running-kit-list'             => array( 'Lista de equipamento para corrida de montanha', 'Guias de compra', array(
			'A maioria das provas de montanha exige proteção impermeável completa com costuras seladas, gorro, luvas, mapa e bússola.',
			'A Jaqueta de corrida Wasdale atende essa regra com 150 g. Use a Camiseta de corrida Wansfell por baixo e o Tênis de trilha Ennerdale nos pés; leve tudo no Colete de hidratação Wasdale 12 litros. Um Gorro de merino Catbells pesa 45 g.',
		) ),
		'winter-hillwalking-kit'             => array( 'O que levar para a montanha no inverno', 'Guias de compra', array(
			'Inverno na montanha britânica quer dizer dias curtos, neve no alto e vento que rouba o calor. Leve mais do que acha que vai precisar.',
			'Use a Bota de inverno Kirkstone com grampões e as Polainas Borrowdale. Leve a Jaqueta de pluma Blencathra, as Luvas impermeáveis Helvellyn e um par reserva, e a Lanterna de cabeça Wasdale: às quatro já escureceu.',
		) ),
		'first-wild-camp-kit'                => array( 'Seu primeiro acampamento selvagem: o que levar', 'Guias de compra', array(
			'Abrigo, sono e algo quente são as três coisas de que um primeiro acampamento selvagem precisa. O resto é conforto.',
			'A Barraca individual Haystacks, o Saco de dormir de pluma três estações Skiddaw e um Isolante inflável Grisedale cuidam do abrigo e do sono. O Fogareiro Fairfield e o Kit de panelas Langdale fervem a água do chá em quatro minutos. Guarde o saco de dormir em um Kit de sacos estanques Haystacks dentro da Mochila Scafell 45 litros.',
		) ),
		'family-walking-kit'                 => array( 'Trilha com crianças: o equipamento que ajuda', 'Guias de compra', array(
			'Crianças sentem frio mais rápido que adultos e se molham antes. Uma mochila só delas faz a trilha ser delas.',
			'A Mochila infantil Catbells 20 litros tem apito na alça peitoral. Guarde nela camadas extras e lanches, e leve a Garrafa térmica Ennerdale com algo quente. Para uma noite fora juntos, a Barraca familiar Langdale tem espaço para ficar de pé.',
		) ),
		'travel-light-kit'                   => array( 'Viajar leve com equipamento outdoor', 'Guias de compra', array(
			'Equipamento outdoor é ótimo para viajar: é leve, seca rápido e não amassa.',
			'A Mochila de viagem Borrowdale cabe nos limites da bagagem de mão e abre como mala. A Primeira camada de merino Coniston e o Suéter de lã Grisedale continuam frescos por dias. Um Liner de seda Kirkstone mantém a roupa de cama do albergue a distância.',
		) ),
		'how-to-reproof-a-waterproof-jacket' => array( 'Como lavar e reimpermeabilizar uma jaqueta impermeável', 'Como fazer', array(
			'Quando a chuva encharca o tecido de fora em vez de formar gotas, a jaqueta está saturando: ela ainda segura a água, mas você vai se sentir úmido com o próprio suor.',
			'Lave a 30 °C com um limpador técnico, nunca com amaciante. Seque na secadora em temperatura baixa por vinte minutos: o calor reativa o acabamento. Se a água ainda encharcar, aplique um spray reimpermeabilizante. Vale para a Jaqueta de chuva leve Catbells, a Jaqueta de montanha Helvellyn e a Calça impermeável Skiddaw.',
		) ),
		'how-to-wash-a-down-jacket'          => array( 'Como lavar uma jaqueta de pluma', 'Como fazer', array(
			'A pluma embola quando molha, e uma jaqueta de pluma lavada como se fosse de algodão sai murcha.',
			'Use sabão para pluma a 30 °C no ciclo delicado, enxágue duas vezes e seque na secadora em temperatura baixa com duas bolinhas de tênis limpas até não sobrar nenhum bolo, o que leva horas. A Jaqueta de pluma Blencathra e a Parka Bowfell voltam ao volume total assim.',
		) ),
		'how-to-care-for-leather-boots'      => array( 'Como cuidar de botas de couro', 'Como fazer', array(
			'Couro que seca duro racha, e couro rachado deixa a água entrar.',
			'Escove a lama enquanto ainda está úmida, seque as botas em temperatura ambiente com jornal dentro, nunca perto do fogo, e passe cera para couro quando estiverem secas. A Bota de trilha impermeável Scafell e a Bota de inverno Kirkstone duram anos assim.',
		) ),
		'how-to-pitch-a-tent-in-rain'        => array( 'Como montar a barraca na chuva', 'Como fazer', array(
			'O segredo é manter o quarto seco, e isso começa em casa: treine uma vez no quintal.',
			'Uma barraca que se monta pelo sobreteto primeiro, como a Barraca individual Haystacks ou a Barraca para duas pessoas Glaramara, sobe com o quarto preso por dentro. Estaqueie primeiro o lado de onde vem o vento, depois as varetas, depois o resto. Deixe as roupas molhadas no avancê.',
		) ),
		'how-to-fit-a-backpack'              => array( 'Como ajustar a mochila', 'Como fazer', array(
			'Uma mochila carregada nos ombros dói em menos de uma hora. A barrigueira deve levar a maior parte do peso.',
			'Afrouxe todas as tiras, ponha a barrigueira sobre o alto dos ossos do quadril e aperte, depois as alças de ombro, depois os reguladores de carga até uns 45 graus. A Mochila cargueira Ennerdale 65 litros e a Mochila Scafell 45 litros têm costas com altura ajustável: regule antes da primeira trilha.',
		) ),
		'how-to-store-a-sleeping-bag'        => array( 'Como guardar um saco de dormir', 'Como fazer', array(
			'Um saco de dormir guardado meses no saco de compressão perde o volume para sempre.',
			'Guarde-o solto em um saco de armazenamento grande ou pendurado no armário. Seque-o por completo depois de cada viagem. O Saco de dormir de pluma três estações Skiddaw e o Saco de dormir de pluma de inverno Bowfell vêm com saco de armazenamento justamente para isso.',
		) ),
		'how-to-use-trekking-poles'          => array( 'Como usar bastões de caminhada', 'Como fazer', array(
			'Os bastões tiram peso dos joelhos na descida e dão um ponto de apoio em terreno irregular.',
			'Regule-os para o cotovelo ficar em ângulo reto no plano, mais curtos na subida e mais longos na descida. Os Bastões de caminhada Scafell vão de 105 a 135 cm, com travas rápidas que se usam de luvas.',
		) ),
		'how-to-stay-warm-at-night-camping'  => array( 'Como não passar frio à noite no acampamento', 'Como fazer', array(
			'A maioria das noites frias é fria por baixo. Um bom isolante importa mais que um saco mais quente.',
			'Durma sobre um Isolante inflável Grisedale, troque para roupas secas antes de deitar e coma algo logo antes de dormir. Um Liner de seda Kirkstone acrescenta uns dois graus, e uma bebida quente da Garrafa térmica Ennerdale ajuda às três da manhã.',
		) ),
		'how-to-dry-wet-boots'               => array( 'Como secar botas molhadas durante a noite', 'Como fazer', array(
			'Botas secas perto do fogo ou do aquecedor racham e descolam.',
			'Tire as palmilhas, encha as botas de jornal e troque depois de uma hora, depois deixe-as em um lugar quente e arejado. O Tênis de trilha Ennerdale dispensa tudo isso: escorre a água e seca enquanto você anda.',
		) ),
		'how-to-read-a-hydrostatic-head'     => array( 'O que é coluna d’água', 'Como fazer', array(
			'Coluna d’água é a altura, em milímetros, de uma coluna de água que um tecido segura antes de vazar. Quanto mais, mais impermeável.',
			'O sobreteto de uma barraca precisa de cerca de 3.000 mm, o piso de mais, porque você se ajoelha nele. Uma jaqueta precisa de 10.000 mm para pancadas e de 20.000 mm ou mais para tempestade: a Jaqueta de montanha Helvellyn tem 28.000 mm, a Jaqueta impermeável do dia a dia Borrowdale, 10.000 mm.',
		) ),
	),

	// Words a Brazilian shopper uses for the same thing.
	'synonyms'   => array(
		array( 'capa de chuva', 'jaqueta de chuva', 'jaqueta impermeável', 'corta-chuva' ),
		array( 'puffer', 'jaqueta de pluma', 'jaqueta térmica', 'jaqueta de nylon' ),
		array( 'mochila', 'cargueira', 'mochila de ataque' ),
		array( 'blusa de frio', 'suéter', 'fleece', 'moletom' ),
	),
);
