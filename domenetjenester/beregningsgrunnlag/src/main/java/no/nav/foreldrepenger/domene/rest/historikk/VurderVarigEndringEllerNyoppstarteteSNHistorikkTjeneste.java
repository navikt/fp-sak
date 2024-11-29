package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;

@ApplicationScoped
public class VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste {

    private Historikkinnslag2Repository historikkinnslagRepository;

    VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste() {
        // CDI
    }

    @Inject
    public VurderVarigEndringEllerNyoppstarteteSNHistorikkTjeneste(Historikkinnslag2Repository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, VurderVarigEndringEllerNyoppstartetSNDto dto) {
        var ref = param.getRef();
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .addTekstlinje(fraTilEquals("Endring i næringsvirksomhet", null, konvertBooleanTilFaktaEndretVerdiType(dto.getErVarigEndretNaering())))
            .addTekstlinje(fraTilEquals("Brutto næringsinntekt", null, dto.getBruttoBeregningsgrunnlag()))
            .addTekstlinje(dto.getBegrunnelse())
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
