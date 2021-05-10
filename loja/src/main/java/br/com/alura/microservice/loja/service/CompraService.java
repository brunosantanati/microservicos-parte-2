package br.com.alura.microservice.loja.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import br.com.alura.microservice.loja.client.FornecedorClient;
import br.com.alura.microservice.loja.client.TransportadorClient;
import br.com.alura.microservice.loja.dto.CompraDTO;
import br.com.alura.microservice.loja.dto.InfoEntregaDTO;
import br.com.alura.microservice.loja.dto.InfoFornecedorDTO;
import br.com.alura.microservice.loja.dto.InfoPedidoDTO;
import br.com.alura.microservice.loja.dto.VoucherDTO;
import br.com.alura.microservice.loja.model.Compra;
import br.com.alura.microservice.loja.model.CompraState;
import br.com.alura.microservice.loja.repository.CompraRepository;

@Service
public class CompraService {
	
	@Autowired
	private FornecedorClient fornecedorClient;
	
	@Autowired
	private TransportadorClient transportadorClient;
	
	@Autowired
	private CompraRepository compraRepository;
	
	@HystrixCommand(threadPoolKey = "getByIdThreadPool")
	public Compra getById(Long id) {
		return compraRepository.findById(id).orElse(new Compra());
	}
	
	
	/*
	 * Comentário extraído da atividade na Alura, referente a reprocessamento e cancelamento:
	 * 
	 * Como vimos, a Loja possui duas responsabilidades: a de executar todos os passos necessários para que a compra seja realizada e o de manter os dados da compra. 
	 * Quando é falado que o cliente deve se responsabilizar pela decisão do que fazer em caso de erro, seja para cancelar a compra ou reprocessar a mesma, 
	 * quem seria esse cliente?
	 * 
	 * Uma outra funcionalidade que tenha a lógica de orquestração da transação de compra
	 * Alternativa correta! O método realizaCompra sabe exatamente todos os passos necessários para que uma compra seja efetivada. 
	 * O que precisamos além disso é quem saiba o que fazer quando esta compra não chega no estado final. 
	 * Cancelar, reprocessar ou tentar mais n vezes? Atualmente, nós temos o CompraController chamando o CompraService.
	 * Poderíamos criar mais um serviço e colocar entre o controller e o serviço de compra. 
	 * Teríamos então o CompraController chamando o OrquestradorDeCompraService, que chamaria o CompraService. 
	 * De acordo com o estado da compra retornada pelo CompraService, o orquestrador decidiria reprocessar, cancelar, etc.
	 */
	
	/**
	 * Caso a compra não seja processada corretamente, o cliente pode tentar reprocessa-la a partir do último estado da compra
	 */
	public Compra reprocessaCompra(Long id) {
		//aqui viria a implementação
		return null;
	}
	
	/**
	 * Caso a compra não seja processada corretamente na primeira vez ou caso o cliente tente reprocessar a compra várias vezes
	 * e não consiga, ele pode cancelar a compra
	 */
	public Compra cancelaCompra(Long id) {
		//aqui viria a implementação
		return null;
	}

	@HystrixCommand(fallbackMethod = "realizaCompraFallback",
			threadPoolKey = "realizaCompraThreadPool")
	public Compra realizaCompra(CompraDTO compra) {
		
		Compra compraSalva = new Compra();
		compraSalva.setState(CompraState.RECEBIDO);
		compraSalva.setEnderecoDestino(compra.getEndereco().toString());
		compraRepository.save(compraSalva);
		compra.setCompraId(compraSalva.getId());
		
		InfoFornecedorDTO info = fornecedorClient.getInfoPorEstado(compra.getEndereco().getEstado());
		InfoPedidoDTO infoPedido = fornecedorClient.realizaPedido(compra.getItens());
		compraSalva.setState(CompraState.PEDIDO_REALIZADO);
		compraSalva.setPedidoId(infoPedido.getId());
		compraSalva.setTempoDePreparo(infoPedido.getTempoDePreparo());
		compraRepository.save(compraSalva);
		
		InfoEntregaDTO entregaDTO = new InfoEntregaDTO();
		entregaDTO.setPedidoId(infoPedido.getId());
		entregaDTO.setDataParaEntrega(LocalDate.now().plusDays(infoPedido.getTempoDePreparo()));
		entregaDTO.setEnderecoOrigem(info.getEndereco());
		entregaDTO.setEnderecoDestino(compra.getEndereco().toString());
		VoucherDTO voucher = transportadorClient.reservaEntrega(entregaDTO);
		compraSalva.setState(CompraState.RESERVA_ENTREGA_REALIZADA);
		compraSalva.setDataParaEntrega(voucher.getPrevisaoParaEntrega());
		compraSalva.setVoucher(voucher.getNumero());
		compraRepository.save(compraSalva);
				
		/*System.out.println(info.getEndereco());
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		return compraSalva;
	}
	
	public Compra realizaCompraFallback(CompraDTO compra) {
		
		if(compra.getCompraId() != null) {
			return compraRepository.findById(compra.getCompraId()).get();
		}
		
		Compra compraFallback = new Compra();
		compraFallback.setEnderecoDestino(compra.getEndereco().toString());
		return compraFallback;
	}
	
}
