package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

public record RettigheterAnnenforelderDto(RettighetDto rettighetAnnenforelder,
                                          RettighetDto rettighetAnnenforelderUføretrygd,
                                          boolean skalAvklareAnnenforelderUføretrygd,
                                          RettighetDto rettighetAnnenforelderStønadEØS,
                                          boolean skalAvklareAnnenforelderStønadEØS) {
}
