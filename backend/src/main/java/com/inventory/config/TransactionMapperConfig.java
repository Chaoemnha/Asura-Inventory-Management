package com.inventory.config;

import com.inventory.dto.TransactionDTO;
import com.inventory.entity.Transaction;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionMapperConfig {

    @Bean(name = "transactionMapper")
    public ModelMapper transactionMapper(){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD);
        modelMapper.addMappings(new PropertyMap<Transaction, TransactionDTO>() {
            @Override
            protected void configure() {
                skip(destination.getUser());
                skip(destination.getProduct());
                skip(destination.getSupplier());
            }
        });
        return modelMapper;
    }
}
