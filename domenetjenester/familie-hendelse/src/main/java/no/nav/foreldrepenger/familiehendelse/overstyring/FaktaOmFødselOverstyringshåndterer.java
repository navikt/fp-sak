package no.nav.foreldrepenger.familiehendelse.overstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.familiehendelse.FaktaFødselTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OverstyringFaktaOmFødselDto;

import java.util.Optional;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaOmFødselDto.class, adapter = Overstyringshåndterer.class)
public class FaktaOmFødselOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaOmFødselDto> {
    private HistorikkinnslagRepository historikkRepository;
    private FaktaFødselTjeneste faktaFødselTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;


    FaktaOmFødselOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaOmFødselOverstyringshåndterer(HistorikkinnslagRepository historikkRepository,
                                              FaktaFødselTjeneste faktaFødselTjeneste,
                                              FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkRepository = historikkRepository;
        this.faktaFødselTjeneste = faktaFødselTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaOmFødselDto dto, BehandlingReferanse ref) {
        var familieHendelse = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        var resultat = faktaFødselTjeneste.overstyrFaktaOmFødsel(ref, familieHendelse, Optional.of(dto.getTermindato()), dto.getBarn());

        var historikkinnslag = faktaFødselTjeneste.lagHistorikkForBarn(ref, familieHendelse, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            true);
        historikkRepository.lagre(historikkinnslag.build());
        return resultat;
    }
}
