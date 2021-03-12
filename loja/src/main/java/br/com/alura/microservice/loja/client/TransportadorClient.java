package br.com.alura.microservice.loja.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import br.com.alura.microservice.loja.dto.InfoEntregaDTO;
import br.com.alura.microservice.loja.dto.VoucherDTO;

@FeignClient("transportador")
public interface TransportadorClient {

	@RequestMapping(method=RequestMethod.POST, path="/entrega")
	public VoucherDTO reservaEntrega(InfoEntregaDTO entregaDTO);
	
}
