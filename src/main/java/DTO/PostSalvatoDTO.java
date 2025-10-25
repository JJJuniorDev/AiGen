package DTO;


import java.time.LocalDateTime;

public class PostSalvatoDTO {
 private Long id;
 private String contenuto;
 private String tipo;
 private String piattaforma;
 private String categoria;
 private Long brandId;
 private String brandName;
 private LocalDateTime dataSalvataggio;
 
 // Costruttori
 public PostSalvatoDTO() {}
 
 public PostSalvatoDTO(Long id, String contenuto, String tipo, String piattaforma, 
                      String categoria, Long brandId, String brandName, LocalDateTime dataSalvataggio) {
     this.id = id;
     this.contenuto = contenuto;
     this.tipo = tipo;
     this.piattaforma = piattaforma;
     this.categoria = categoria;
     this.brandId = brandId;
     this.brandName = brandName;
     this.dataSalvataggio = dataSalvataggio;
 }
 
 // Getter e Setter
 public Long getId() { return id; }
 public void setId(Long id) { this.id = id; }
 
 public String getContenuto() { return contenuto; }
 public void setContenuto(String contenuto) { this.contenuto = contenuto; }
 
 public String getTipo() { return tipo; }
 public void setTipo(String tipo) { this.tipo = tipo; }
 
 public String getPiattaforma() { return piattaforma; }
 public void setPiattaforma(String piattaforma) { this.piattaforma = piattaforma; }
 
 public String getCategoria() { return categoria; }
 public void setCategoria(String categoria) { this.categoria = categoria; }
 
 public Long getBrandId() { return brandId; }
 public void setBrandId(Long brandId) { this.brandId = brandId; }
 
 public String getBrandName() { return brandName; }
 public void setBrandName(String brandName) { this.brandName = brandName; }
 
 public LocalDateTime getDataSalvataggio() { return dataSalvataggio; }
 public void setDataSalvataggio(LocalDateTime dataSalvataggio) { this.dataSalvataggio = dataSalvataggio; }
}