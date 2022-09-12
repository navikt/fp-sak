package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

public record RettigheterAnnenforelderDto(Boolean bekreftetAnnenforelderRett,
                                          Boolean bekreftetAnnenForelderRettEØS,
                                          boolean skalAvklareAnnenForelderRettEØS,
                                          Boolean bekreftetAnnenforelderUføretrygd,
                                          boolean skalAvklareAnnenforelderUføretrygd,
                                          Boolean bekreftetAnnenforelderStønadEØS,
                                          boolean skalAvklareAnnenforelderStønadEØS) {
}
