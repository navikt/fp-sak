package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

public record RettigheterAnnenforelderDto(Boolean bekreftetAnnenforelderRett,
                                          Boolean bekreftetAnnenforelderUføretrygd,
                                          boolean skalAvklareAnnenforelderUføretrygd,
                                          Boolean bekreftetAnnenforelderStønadEØS,
                                          boolean skalAvklareAnnenforelderStønadEØS) {
}
