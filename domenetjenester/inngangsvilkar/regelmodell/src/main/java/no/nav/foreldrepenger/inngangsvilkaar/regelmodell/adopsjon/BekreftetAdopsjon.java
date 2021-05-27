package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import java.time.LocalDate;
import java.util.List;

public record BekreftetAdopsjon(LocalDate omsorgsovertakelseDato,
                                List<BekreftetAdopsjonBarn> adopsjonBarn,
                                boolean ektefellesBarn,
                                boolean adoptererAlene) {
}
