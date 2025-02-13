package no.nav.foreldrepenger.behandling.steg.kompletthet.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetSteg;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@BehandlingStegRef(BehandlingStegType.VURDER_KOMPLETT_TIDLIG)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VurderSøktForTidligStegImpl implements VurderKompletthetSteg {

    private Kompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderSøktForTidligStegImpl() {
    }

    @Inject
    public VurderSøktForTidligStegImpl(Kompletthetsjekker kompletthetsjekker,
                                       BehandlingRepositoryProvider provider,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        if (skalPassereKompletthet(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var søknadMottatt = kompletthetsjekker.vurderSøknadMottattForTidlig(ref, skjæringstidspunkter);
        if (!søknadMottatt.erOppfylt() && skalVenteDersomSøktForTidlig(behandling, skjæringstidspunkter)) {
            return VurderKompletthetStegFelles.evaluerUoppfylt(søknadMottatt, VENT_PGA_FOR_TIDLIG_SØKNAD);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVenteDersomSøktForTidlig(Behandling behandling, Skjæringstidspunkt stp) {
        if (!kanPassereKompletthet(behandling)) {
            return true;
        }
        if (RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType())) {
            return false;
        }
        var søknadFHDato = stp.getFamilieHendelseDato().map(FamilieHendelseDato::familieHendelseDato).orElse(null);
        var farRundtFødselTom = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(false, true, søknadFHDato, søknadFHDato)
            .map(LocalDateInterval::getTomDato).orElse(søknadFHDato);
        var farØnskerJustert = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(YtelseFordelingAggregat::getGjeldendeFordeling)
            .filter(OppgittFordelingEntitet::ønskerJustertVedFødsel)
            .filter(fordeling -> erFørsteUttaksdatoRundtFødsel(fordeling, farRundtFødselTom))
            .isPresent();
        return !farØnskerJustert;
    }

    private boolean erFørsteUttaksdatoRundtFødsel(OppgittFordelingEntitet oppgittFordeling, LocalDate fødselsperiodeTom) {
        return fødselsperiodeTom != null && oppgittFordeling.getPerioder().stream()
            .filter(periode -> !(periode.isUtsettelse() || periode.isOpphold()))
            .anyMatch(p -> p.getFom().isBefore(fødselsperiodeTom));
    }

}
