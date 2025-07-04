package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class OppdaterAvslutningsdatoFagsakRelasjon {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private Instance<UtledeAvslutningsdatoFagsak> utledeAvslutningsdatoFagsak;


    @Inject
    public OppdaterAvslutningsdatoFagsakRelasjon(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                                 @Any Instance<UtledeAvslutningsdatoFagsak> utledeAvslutningsdatoFagsak) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.utledeAvslutningsdatoFagsak = utledeAvslutningsdatoFagsak;
    }

    public OppdaterAvslutningsdatoFagsakRelasjon () {
        //CDI proxy
    }

    public void oppdaterFagsakRelasjonAvslutningsdato(FagsakRelasjon relasjon,
                                                      Long fagsakId,
                                                      Optional<FagsakLås> fagsak1Lås,
                                                      Optional<FagsakLås> fagsak2Lås,
                                                      FagsakYtelseType ytelseType) {
        var utlederAvslutningsdato = FagsakYtelseTypeRef.Lookup.find(this.utledeAvslutningsdatoFagsak, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner av UtledeAvslutningsdatoFagsak funnet for ytelse: " + ytelseType.getKode()));

        var avslutningsdato = utlederAvslutningsdato.utledAvslutningsdato(fagsakId, relasjon);
        fagsakRelasjonTjeneste.oppdaterMedAvslutningsdato(relasjon, avslutningsdato, fagsak1Lås, fagsak2Lås);
    }

}
