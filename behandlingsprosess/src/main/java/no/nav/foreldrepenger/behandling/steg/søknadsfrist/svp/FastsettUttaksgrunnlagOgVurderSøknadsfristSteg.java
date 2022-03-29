package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.svp.FørsteLovligeUttaksdatoTjeneste;

//TODO(SVP-team) rename steget.. skal ikke ha ytelsespsifikke stegnavn..
@BehandlingStegRef(kode = BehandlingStegKoder.SØKNADSFRIST_FORELDREPENGER_KODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class FastsettUttaksgrunnlagOgVurderSøknadsfristSteg implements BehandlingSteg {

    private SøktPeriodeTjeneste søktPeriodeTjeneste;
    private FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private UttakInputTjeneste uttakInputTjeneste;

    public FastsettUttaksgrunnlagOgVurderSøknadsfristSteg() {
        // For CDI
    }

    @Inject
    public FastsettUttaksgrunnlagOgVurderSøknadsfristSteg(BehandlingRepositoryProvider behandlingRepositoryProvider,
            UttakInputTjeneste uttakInputTjeneste,
            @FagsakYtelseTypeRef("SVP") SøktPeriodeTjeneste søktPeriodeTjeneste,
            FørsteLovligeUttaksdatoTjeneste førsteLovligeUttaksdatoTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.søktPeriodeTjeneste = søktPeriodeTjeneste;
        this.førsteLovligeUttaksdatoTjeneste = førsteLovligeUttaksdatoTjeneste;
        this.uttaksperiodegrenseRepository = behandlingRepositoryProvider.getUttaksperiodegrenseRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var input = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        var uttaksgrenserOptional = søktPeriodeTjeneste.finnSøktPeriode(input);
        if (uttaksgrenserOptional.isPresent()) {
            var søknadsfristResultat = førsteLovligeUttaksdatoTjeneste.utledFørsteLovligeUttaksdato(input, uttaksgrenserOptional.get());
            // Opprett aksjonspunkt dersom regel ikke er oppfylt.
            var årsakKodeIkkeVurdert = søknadsfristResultat.getÅrsakKodeIkkeVurdert();
            if (!søknadsfristResultat.isRegelOppfylt() && årsakKodeIkkeVurdert.isPresent()) {
                var aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(årsakKodeIkkeVurdert.get());
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
            BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, førsteSteg)) {
            uttaksperiodegrenseRepository.ryddUttaksperiodegrense(kontekst.getBehandlingId());
        }
    }
}
