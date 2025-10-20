package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private BeregningTjeneste beregningTjeneste;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository,
                          BehandlingsresultatRepository behandlingsresultatRepository,
                          KlageAnkeVedtakTjeneste klageAnkeVedtakTjeneste,
                          SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste,
                          DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                          FagsakEgenskapRepository fagsakEgenskapRepository,
                          BeregningTjeneste beregningTjeneste) {
        this.sjekkMotEksisterendeOppgaverTjeneste = sjekkMotEksisterendeOppgaverTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.klageAnkeVedtakTjeneste = klageAnkeVedtakTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.beregningTjeneste = beregningTjeneste;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling) {
        return foreslåVedtak(behandling, List.of());
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, Collection<AksjonspunktDefinisjon> aksjonspunkterFraSteg) {
        var fagsakId = behandling.getFagsakId();
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (fagsak.erStengt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>(aksjonspunkterFraSteg);
        if (KlageAnkeVedtakTjeneste.behandlingErKlageEllerAnke(behandling)) {
            if (klageAnkeVedtakTjeneste.erKlageResultatHjemsendt(behandling) || klageAnkeVedtakTjeneste.erBehandletAvKabal(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        } else {
            aksjonspunktDefinisjoner
                    .addAll(sjekkMotEksisterendeOppgaverTjeneste.sjekkMotEksisterendeGsakOppgaver(behandling.getAktørId(), behandling));
            if (behandling.erRevurdering() && behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING) &&
                harÅpneKlagerEllerAnker(behandling.getFagsak())) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.VURDERE_INNTEKTSMELDING_FØR_VEDTAK);
            }
        }

        if (skalNullstilleUtfyllendeFritekstForVedtak(behandling)) {
            dokumentBehandlingTjeneste.nullstillVedtakFritekstHvisFinnes(behandling.getId());
        }

        håndterToTrinn(behandling, aksjonspunktDefinisjoner);

        if (harTidligereOverstyringAvVedtaksbrevUtenAtDetBlirAksjonspunktForeslåVedtak(behandling, aksjonspunktDefinisjoner)) {
            if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.FORESLÅ_VEDTAK)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
            } else if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
            } else {
                // Skal ikke være mulig å overstyre brev utenom et tidligere avbrutt FORESLÅ_VEDTAK/FORESLÅ_VEDTAK_MANUELT aksjonspunkt
                throw new IllegalStateException("Utviklerfeil: Skal ikke kunne sende ut automatisk fritekstbrev uten totrinn!");
            }
        }

        return aksjonspunktDefinisjoner.isEmpty() ? BehandleStegResultat.utførtUtenAksjonspunkter()
                : BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
    }

    private boolean skalNullstilleUtfyllendeFritekstForVedtak(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return false;
        }

        if (!skalUtføreTotrinnsbehandling(behandling)) {
            return true;
        }

        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultatOpt.isEmpty() || !behandlingsresultatOpt.get().isBehandlingInnvilget()) {
            return false;
        }

        var aksjonspunktAvvikIBeregning = behandling.getAksjonspunkter().stream()
            .filter(ap -> AksjonspunktDefinisjon.getAvvikIBeregning().contains(ap.getAksjonspunktDefinisjon()))
            .toList();
        var harAksjonspunktAvvikIBeregningOgAlleErAvbrutt = aksjonspunktAvvikIBeregning.isEmpty() || aksjonspunktAvvikIBeregning.stream().allMatch(Aksjonspunkt::erAvbrutt);
        return harAksjonspunktAvvikIBeregningOgAlleErAvbrutt && !harManueltFastsatteAndelerPåStp(behandling);
    }

    private boolean harManueltFastsatteAndelerPåStp(Behandling behandling) {
        var beregningsgrunnlagOpt = beregningTjeneste.hent(BehandlingReferanse.fra(behandling))
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        if (beregningsgrunnlagOpt.isEmpty()) {
            return false;
        }
        var beregningsgrunnlag = beregningsgrunnlagOpt.get();
        var skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();
        var periodePåStp = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getPeriode().inkluderer(skjæringstidspunkt))
            .findFirst()
            .orElseThrow();
        return periodePåStp.getBeregningsgrunnlagPrStatusOgAndelList().stream().anyMatch(a -> a.getOverstyrtPrÅr() != null);
    }

    private boolean harTidligereOverstyringAvVedtaksbrevUtenAtDetBlirAksjonspunktForeslåVedtak(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (aksjonspunktDefinisjoner.contains(AksjonspunktDefinisjon.FORESLÅ_VEDTAK) || aksjonspunktDefinisjoner.contains(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT)) {
            return false;
        }
        return dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandling.getId()).isPresent();
    }

    private boolean harÅpneKlagerEllerAnker(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .anyMatch(KlageAnkeVedtakTjeneste::behandlingErKlageEllerAnke);
    }

    private void håndterToTrinn(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (skalUtføreTotrinnsbehandling(behandling)) {
            if (!behandling.isToTrinnsBehandling()) {
                behandling.setToTrinnsBehandling();
                LOG.info("To-trinn satt på behandling={}", behandling.getId());
            }
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        } else {
            behandling.nullstillToTrinnsBehandling();
            LOG.info("To-trinn fjernet på behandling={}", behandling.getId());
            if (skalOppretteForeslåVedtakManuelt(behandling)) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
            }
        }
    }

    private boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        if (behandling.erRevurdering() && behandling.erManueltOpprettet()) {
            return true;
        }
        if (behandling.harNoenBehandlingÅrsaker(BehandlingÅrsakType.årsakerRelatertTilDød())) {
            return true;
        }
        if (behandling.harNoenBehandlingÅrsaker(Set.of(BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, BehandlingÅrsakType.FEIL_PRAKSIS_IVERKS_UTSET)) ||
            fagsakEgenskapRepository.harFagsakMarkering(behandling.getFagsakId(), FagsakMarkering.PRAKSIS_UTSETTELSE)) {
            return true;
        }
        if (harAksjonspunktUtførtAvSaksbehandler(behandling)) {
            return true;
        }
        return avslagEllerOpphørInngangsvilkår(behandling);
    }

    private boolean skalUtføreTotrinnsbehandling(Behandling behandling) {
        return behandling.harAksjonspunktMedTotrinnskontroll();
    }

    private boolean harAksjonspunktUtførtAvSaksbehandler(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .anyMatch(this::aksjonspunktUtførtAvSaksbehandler);
    }

    private boolean aksjonspunktUtførtAvSaksbehandler(Aksjonspunkt aksjonspunkt) {
        var aksjonspunktDefinisjon = aksjonspunkt.getAksjonspunktDefinisjon();
        return aksjonspunkt.erUtført() &&
            !AksjonspunktType.AUTOPUNKT.equals(aksjonspunktDefinisjon.getAksjonspunktType()) &&
            !AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.equals(aksjonspunktDefinisjon) &&
            Optional.ofNullable(aksjonspunkt.getEndretAv()).map(String::toLowerCase).filter(s -> s.startsWith("srv")).isEmpty();
    }

    private boolean avslagEllerOpphørInngangsvilkår(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(this::avslagEllerOpphørInngangsvilkår)
            .isPresent();
    }

    // Avslag eller opphørt med ett inngangsvilkår ikke oppfylt. Ikke aksjonspunkt ved opphør pga uttaksregler
    private boolean avslagEllerOpphørInngangsvilkår(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.isBehandlingsresultatAvslått() ||
            (behandlingsresultat.isBehandlingsresultatOpphørt() &&
                behandlingsresultat.getVilkårResultat().hentAlleGjeldendeVilkårsutfall().contains(VilkårUtfallType.IKKE_OPPFYLT));
    }

}
