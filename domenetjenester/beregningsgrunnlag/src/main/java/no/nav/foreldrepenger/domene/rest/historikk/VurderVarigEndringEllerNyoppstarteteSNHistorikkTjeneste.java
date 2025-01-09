package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;

@ApplicationScoped
public class VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VurderVarigEndringEllerNyoppstartetSNDto dto) {
        var ref = param.getRef();
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .addLinje(fraTilEquals("Endring i næringsvirksomhet", null, konvertBooleanTilFaktaEndretVerdiType(dto.getErVarigEndretNaering())))
            .addLinje(fraTilEquals("Brutto næringsinntekt", null, dto.getBruttoBeregningsgrunnlag()))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static String konvertBooleanTilFaktaEndretVerdiType(Boolean endringNæring) {
        if (endringNæring == null) {
            return null;
        }
        return endringNæring ? "Varig endret eller nystartet næring" : "Ingen varig endret eller nyoppstartet næring";
    }
}
