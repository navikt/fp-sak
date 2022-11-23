package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.fp;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class EtterkontrollTjenesteImpl implements EtterkontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(EtterkontrollTjenesteImpl.class);
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private RevurderingHistorikk revurderingHistorikk;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjeneste revurderingTjeneste;

    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    EtterkontrollTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public EtterkontrollTjenesteImpl(HistorikkRepository historikkRepository,
            BehandlingRevurderingRepository behandlingRevurderingRepository,
            ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingHistorikk = new RevurderingHistorikk(historikkRepository);
        this.revurderingTjeneste = revurderingTjeneste;
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public Optional<BehandlingÅrsakType> utledRevurderingÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag,
            List<FødtBarnInfo> barnFraRegister) {
        if (grunnlag == null) {
            return Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
        }

        return utledRevurderingsÅrsak(grunnlag, barnFraRegister.size());
    }

    @Override
    public void opprettRevurdering(Behandling behandling, boolean skalAnnenpartEtterkontrolleres, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering) {
        var fagsak = behandling.getFagsak();
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, årsak, enhetForRevurdering);

        var behandlingMedforelder = behandlingRevurderingRepository
                .finnSisteInnvilgetBehandlingForMedforelder(behandling.getFagsak());

        if (behandlingMedforelder.isPresent() && skalAnnenpartEtterkontrolleres) {
            // For dette tilfellet vil begge sakene etterkontrolleres samtidig.
            LOG.info("Etterkontroll har funnet fagsak (id={}) på medforelder for fagsak med fagsakId={}", behandlingMedforelder.get().getFagsakId(),
                    fagsak.getId());
            var denneStarterUttakFørst = denneForelderHarTidligstUttak(behandling, behandlingMedforelder.get());

            if (denneStarterUttakFørst || BehandlingÅrsakType.RE_HENDELSE_FØDSEL.equals(årsak)) { // Først eller etterregistrerte foreldre
                behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
            } else {
                enkøBehandling(revurdering);
                revurderingHistorikk.opprettHistorikkinnslagForVenteFristRelaterteInnslag(revurdering.getId(), fagsak.getId(),
                        HistorikkinnslagType.BEH_KØET, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
            }
        } else {
            behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        }
    }

    private Optional<BehandlingÅrsakType> utledRevurderingsÅrsak(FamilieHendelseGrunnlagEntitet grunnlag, int antallBarnRegister) {
        var antallBarnSakBekreftet = finnAntallBekreftet(grunnlag);

        if ((antallBarnRegister == 0) && (finnAntallOverstyrtManglendeFødsel(grunnlag) > 0)) {
            return Optional.empty();
        }
        if ((antallBarnSakBekreftet > 0) && (antallBarnSakBekreftet == antallBarnRegister)) {
            return Optional.empty();
        }

        if (antallBarnRegister == 0) {
            return Optional.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        }
        // Tilfelle av etterregistrert far/medmor, eller avvik i antall barn
        return antallBarnSakBekreftet == 0 ? Optional.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                : Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    private int finnAntallBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
        int antallBarn = grunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
        if (antallBarn == 0) {
            return finnAntallOverstyrtManglendeFødsel(grunnlag);
        }
        return antallBarn;
    }

    private int finnAntallOverstyrtManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getOverstyrtVersjon().filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType())).map(FamilieHendelseEntitet::getAntallBarn)
                .orElse(0);
    }

    private boolean denneForelderHarTidligstUttak(Behandling behandling, Behandling annenForelder) {
        var førsteUttaksdato = finnFørsteUttaksdato(behandling.getId());
        var førsteUttaksdatoMedforelder = finnFørsteUttaksdato(annenForelder.getId());
        if (førsteUttaksdatoMedforelder.isPresent() && førsteUttaksdato.isPresent()) {
            return førsteUttaksdatoMedforelder.get().isAfter(førsteUttaksdato.get());
        }
        return førsteUttaksdatoMedforelder.isEmpty();
    }

    private Optional<LocalDate> finnFørsteUttaksdato(Long behandling) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling).flatMap(ForeldrepengerUttak::finnFørsteUttaksdato);
    }

    public void enkøBehandling(Behandling behandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null,
                Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

}
