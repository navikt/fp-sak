package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaOmFødselDto.class, adapter = Overstyringshåndterer.class)
public class FaktaOmFødselOverstyringshåndterer implements Overstyringshåndterer<OverstyringFaktaOmFødselDto> {

    private static final Logger LOG = LoggerFactory.getLogger(FaktaOmFødselOverstyringshåndterer.class);

    private HistorikkinnslagRepository historikkRepository;
    private FaktaFødselTjeneste faktaFødselTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    /*
     * TODO:
    - Sjekk om det er lov å endre fra verdi til null, hvis ikke -> validering i frontend på dette
    - Sende fra backend hva som kan overstyres og ikke
    - Ta med kilde i nytt endepunkt slik at vi kan vise dette i boksene på toppen
     */


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

    private static HistorikkinnslagLinjeBuilder getTerminbekreftelseLinje(OverstyringFaktaOmFødselDto dto,
                                                                          FamilieHendelseGrunnlagEntitet familieHendelse) {
        return fraTilEquals("Termindato", familieHendelse.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null),
            dto.getTermindato());
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaOmFødselDto dto, BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();

        LOG.info("Overstyrer fakta rundt fødsel for behandlingId {} til {}", behandlingId, dto);
        var familieHendelse = familieHendelseTjeneste.hentAggregat(behandlingId);
        faktaFødselTjeneste.overstyrFaktaOmFødsel(ref, dto);
        opprettHistorikkinnslag(ref, dto, familieHendelse);
        return OppdateringResultat.utenOverhopp();
    }

    private void opprettHistorikkinnslag(BehandlingReferanse ref, OverstyringFaktaOmFødselDto dto, FamilieHendelseGrunnlagEntitet familieHendelse) {
        // TODO: Legg inn linjer for barn
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .addLinje(new HistorikkinnslagLinjeBuilder().tekst("Overstyrt fakta om fødsel"))
            .addLinje(fraTilEquals("Antall barn", familieHendelse.getGjeldendeAntallBarn(), dto.getAntallBarn()))
            .addLinje(getTerminbekreftelseLinje(dto, familieHendelse))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkRepository.lagre(historikkinnslag);
    }
}
