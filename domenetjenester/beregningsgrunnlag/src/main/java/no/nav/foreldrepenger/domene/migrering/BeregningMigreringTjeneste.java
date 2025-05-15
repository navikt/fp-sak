package no.nav.foreldrepenger.domene.migrering;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.folketrygdloven.kalkulus.migrering.BeregningsgrunnlagGrunnlagMigreringDto;
import no.nav.folketrygdloven.kalkulus.migrering.MigrerBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.migrering.MigrerBeregningsgrunnlagResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRegelSporing;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.GrunnbeløpReguleringsutleder;
import no.nav.foreldrepenger.domene.prosess.KalkulusKlient;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class BeregningMigreringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningMigreringTjeneste.class);

    private KalkulusKlient klient;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    private BehandlingRepository behandlingRepository;
    private RegelsporingMigreringTjeneste regelsporingMigreringTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    BeregningMigreringTjeneste() {
        // CDI
    }

    @Inject
    public BeregningMigreringTjeneste(KalkulusKlient klient,
                                      BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      BeregningsgrunnlagKoblingRepository koblingRepository,
                                      BehandlingRepository behandlingRepository,
                                      RegelsporingMigreringTjeneste regelsporingMigreringTjeneste,
                                      SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.klient = klient;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.koblingRepository = koblingRepository;
        this.behandlingRepository = behandlingRepository;
        this.regelsporingMigreringTjeneste = regelsporingMigreringTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public boolean skalBeregnesIKalkulus(BehandlingReferanse referanse) {
        if (Environment.current().isProd()) {
            return false;
        }
        var harAktivtGrunnlagIFpsak = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId()).isPresent();
        if (harAktivtGrunnlagIFpsak) {
            return false;
        }
        var originalKobling = referanse.getOriginalBehandlingId().flatMap(koblingRepository::hentKobling);
        if (referanse.erRevurdering() && originalKobling.isEmpty()) {
            // Revurderinger er avhengig av at originalkobling ligger i kalkulus så vi har et grunnlag å basere oss på
            return false;
        }
        return true;
    }

    public void migrerSak(no.nav.foreldrepenger.domene.typer.Saksnummer saksnummer) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(Behandling::erAvsluttet)
            .toList();
        var sorterteBehandlinger = sorterBehandlinger(behandlinger);
        sorterteBehandlinger.forEach(this::migrerBehandling);
    }

    public Set<Behandling> sorterBehandlinger(List<Behandling> behandlinger) {
        LinkedHashSet<Behandling> sortertListe = new LinkedHashSet<>();

        // Legger til behandlinger uten avhengigheter
        behandlinger.stream().filter(b -> b.getOriginalBehandlingId().isEmpty()).forEach(sortertListe::add);

        // Legger til behandlinger som peker på de som blir migrert først
        while (sortertListe.size() != behandlinger.size()) {
            var nyttElementILista = behandlinger.stream()
                .filter(behandling -> !listeInneholderBehandling(sortertListe, behandling.getId()))
                .filter(behandling -> {
                    var originalBehandling = behandling.getOriginalBehandlingId().orElseThrow();
                    return listeInneholderBehandling(sortertListe, originalBehandling);
                }).findFirst();
            if (nyttElementILista.isEmpty()) {
                break;
            }
            sortertListe.add(nyttElementILista.get());
        }
        return sortertListe;
    }

    private boolean listeInneholderBehandling(LinkedHashSet<Behandling> sortertListe, Long id) {
        return sortertListe.stream().anyMatch(b -> b.getId().equals(id));
    }

    private void migrerBehandling(Behandling behandling) {
        var grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId());
        var erHenlagt = behandling.getBehandlingsresultat().getBehandlingResultatType().erHenlagt();
        if (grunnlag.isEmpty()) {
            LOG.info(String.format("Finner ikke beregningsgrunnlag på behandling %s, ingenting å migrere", behandling.getId()));
            return;
        }
        var ref = BehandlingReferanse.fra(behandling);
        try {
            // Map og migrer
            var grunnlagSporinger = regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(grunnlag.get(), ref);
            var originalKobling = behandling.getOriginalBehandlingId().flatMap(oid -> koblingRepository.hentKobling(oid));
            var aksjonspunkter = behandlingRepository.hentBehandling(behandling.getId()).getAksjonspunkter();
            var migreringsDto = BeregningMigreringMapper.map(grunnlag.get(), grunnlagSporinger, aksjonspunkter);
            var kobling = koblingRepository.hentKobling(behandling.getId()).orElseGet(() -> koblingRepository.opprettKobling(ref));
            var request = lagMigreringRequest(ref, kobling, originalKobling, migreringsDto, true);
            var response = klient.migrerGrunnlag(request);

            // Sammenlign grunnlag fra kalkulus og fpsak
            sammenlignGrunnlag(response, ref, grunnlagSporinger, aksjonspunkter, erHenlagt);

            // Oppdater kobling med data fra grunnlag
            if (response.grunnlag() != null && response.grunnlag().getBeregningsgrunnlag() != null) {
                oppdaterKoblingMedStpGrunnbeløpOgReguleringsbehov(kobling, response.grunnlag().getBeregningsgrunnlag());
            }

            Optional<LocalDate> førsteUttaksdato;
            try {
                førsteUttaksdato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId()).getFørsteUttaksdatoHvisFinnes();
            } catch (Exception e) {
                LOG.info("Fant ikke første uttaksdato for behandling {}, setter den til LocalDate.MIN", ref.behandlingUuid());
                førsteUttaksdato = Optional.of(LocalDate.MIN);
            }
            // Må ha false toggle her til fpkalkulus er prodsatt
            if (førsteUttaksdato.filter(this::kanPåvirkesAvÅretsGregulering).isPresent()) {
                migrerAlleInaktiveGrunnlag(ref);
            }

            LOG.info(String.format("Vellykket migrering og verifisering av beregningsgrunnlag på sak %s, behandlingId %s og grunnlag %s.", ref.saksnummer(),
                behandling.getId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId)));
        } catch (Exception e) {
            var msg = String.format("Feil ved mapping av grunnlag for sak %s, behandlingId %s og grunnlag %s. Fikk feil %s", ref.saksnummer(),
                behandling.getUuid(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId), e);
            throw new IllegalStateException(msg);
        }
    }

    private void migrerAlleInaktiveGrunnlag(BehandlingReferanse ref) {
        var kobling = koblingRepository.hentKobling(ref.behandlingId()).orElseThrow(() -> new IllegalStateException("Aktivt grunnlag skal allerede være migrert og ha en eksisterende kobling"));
        Arrays.stream(BeregningsgrunnlagTilstand.values()).forEach(tilstand -> {
            var entitet = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.behandlingId(),
                tilstand);
            var grunnlag = entitet.filter(e -> !e.erAktivt()) // Trenger ikke migrere aktivt grunnlag igjen
                .map(gr -> BeregningMigreringMapper.map(gr, Collections.emptyMap(), Collections.emptySet())); // Sporinger og avklaringsbehov settes kun utifra aktivt grunnlag
            grunnlag.ifPresent(gr -> {
                LOG.info("Migrerer inaktivt grunnlag med tilstand {} for behandling {}", tilstand, ref.behandlingUuid());
                var request = lagMigreringRequest(ref, kobling, Optional.empty(), gr, false);
                var response = klient.migrerGrunnlag(request);
                var fpsakGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(entitet.orElseThrow());
                var kalkulusGrunnlag = KalkulusTilFpsakMapper.map(response.grunnlag(), Optional.ofNullable(response.besteberegningGrunnlag()));
                var fpJson = StandardJsonConfig.toJson(fpsakGrunnlag);
                var kalkJson = StandardJsonConfig.toJson(kalkulusGrunnlag);
                if (!fpJson.equals(kalkJson)) {
                    logg(fpJson, kalkJson, false);
                }
            });

        });
    }

    private boolean kanPåvirkesAvÅretsGregulering(LocalDate førsteUttaksdato) {
        var datoForNyG = LocalDate.of(2025,5,1);
        var nyGDatoMedBuffer = datoForNyG.minusWeeks(2);
        return førsteUttaksdato.isAfter(nyGDatoMedBuffer);
    }

    private void oppdaterKoblingMedStpGrunnbeløpOgReguleringsbehov(BeregningsgrunnlagKobling kobling, BeregningsgrunnlagDto grunnlag) {
        var stp = grunnlag.getSkjæringstidspunkt();
        var grunnbeløp = grunnlag.getGrunnbeløp() == null ? null : Beløp.fra(grunnlag.getGrunnbeløp().verdi());
        var harBehovForGRegulering = grunnbeløp != null && GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(
            KalkulusTilFpsakMapper.mapGrunnlag(grunnlag, Optional.empty()));
        koblingRepository.oppdaterKoblingMedStpOgGrunnbeløp(kobling, grunnbeløp, stp);
        koblingRepository.oppdaterKoblingMedReguleringsbehov(kobling, harBehovForGRegulering);
    }

    private void sammenlignGrunnlag(MigrerBeregningsgrunnlagResponse response, BehandlingReferanse referanse,
                                    Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> grunnlagSporinger,
                                    Set<Aksjonspunkt> aksjonspunkter, boolean erHenlagt) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId()).orElseThrow();
        verifiserRegelsporinger(response, entitet, grunnlagSporinger);
        verifiserAksjonspunkter(aksjonspunkter, response.avklaringsbehov());
        var fpsakGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(entitet);
        var kalkulusGrunnlag = KalkulusTilFpsakMapper.map(response.grunnlag(), Optional.ofNullable(response.besteberegningGrunnlag()));
        var fpJson = StandardJsonConfig.toJson(fpsakGrunnlag);
        var kalkJson = StandardJsonConfig.toJson(kalkulusGrunnlag);
        if (!fpJson.equals(kalkJson)) {
            logg(fpJson, kalkJson, true);
            if (erHenlagt) {
                LOG.info("Sammenligning feilet, men behandling er henlagt, så fortsetter migrering. Saksnummer {} behandlingUUid {}", referanse.saksnummer(), referanse.behandlingUuid());
            } else {
                throw new IllegalStateException("Missmatch mellom kalkulus json og fpsak json av samme beregninsgrunnlag");
            }
        }
    }

    private void verifiserAksjonspunkter(Set<Aksjonspunkt> aksjonspunkter, List<MigrerBeregningsgrunnlagResponse.Avklaringsbehov> alleAvklaringsbehov) {
        var relevanteAksjonspunkter = aksjonspunkter.stream()
            .filter(a -> BeregningAvklaringsbehovMigreringMapper.finnBeregningAvklaringsbehov(a.getAksjonspunktDefinisjon()) != null)
            .toList();
        var erLikeStore = relevanteAksjonspunkter.size() == alleAvklaringsbehov.size();
        var alleAvklaringsbehovMtcher = erLikeStore &&
            relevanteAksjonspunkter.stream().allMatch(aksjonspunkt -> alleAvklaringsbehov.stream().anyMatch(avklaringsbehov -> finnesMatchForAksjonspunkt(avklaringsbehov, aksjonspunkt)));
        if (!alleAvklaringsbehovMtcher) {
            throw new IllegalStateException("Feil med matching av avklaringsbehov");
        }
    }

    private boolean finnesMatchForAksjonspunkt(MigrerBeregningsgrunnlagResponse.Avklaringsbehov avklaringsbehov, Aksjonspunkt aksjonspunkt) {
        var definisjonMatcher = Objects.equals(BeregningAvklaringsbehovMigreringMapper.finnBeregningAvklaringsbehov(aksjonspunkt.getAksjonspunktDefinisjon()), avklaringsbehov.definisjon());
        var statusMatcher = Objects.equals(BeregningAvklaringsbehovMigreringMapper.mapAvklaringStatus(aksjonspunkt.getStatus()), avklaringsbehov.status());
        var begrunnelseMatcher = Objects.equals(aksjonspunkt.getBegrunnelse(), avklaringsbehov.begrunnelse());
        var vurdertAvMatcher = Objects.equals(aksjonspunkt.getEndretAv(), avklaringsbehov.vurdertAv());
        var vurdertTidspunktMatcher = Objects.equals(aksjonspunkt.getEndretTidspunkt(), avklaringsbehov.vurdertTidspunkt());
        return definisjonMatcher && statusMatcher && begrunnelseMatcher && vurdertAvMatcher && vurdertTidspunktMatcher;
    }

    private void logg(String jsonFpsak, String jsonKalkulus, boolean erAktivt) {
        try {
            if (!erAktivt) {
                LOG.info("Sammenligning av inaktivt grunnlag feilet");
            }
            var maskertFpsak = jsonFpsak.replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            var maskertKalkulus = jsonKalkulus.replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            LOG.info("Json fpsak: {}", maskertFpsak);
            LOG.info("Json kalkulus: {}", maskertKalkulus);
        } catch (TekniskException jsonProcessingException) {
            LOG.warn("Feil ved logging av grunnlagsjson", jsonProcessingException);
        }
    }

    private void verifiserRegelsporinger(MigrerBeregningsgrunnlagResponse response, BeregningsgrunnlagGrunnlagEntitet entitet,
                                         Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> fpsakGrunnlagSporinger) {
        // Verifiser grunnlagsporinger
        var grunnlagSporinger = response.sporingerGrunnlag();
        var alleGrunnlagSporingerMatcher = grunnlagSporinger.size() == fpsakGrunnlagSporinger.size() && grunnlagSporinger.stream().allMatch(kalkulusRegelGrunnlag -> {
            var type = KodeverkFraKalkulusMapper.mapRegelGrunnlagType(kalkulusRegelGrunnlag.type());
            var fpsakSporing = fpsakGrunnlagSporinger.get(type);
            return fpsakSporing != null && Objects.equals(fpsakSporing.getRegelEvaluering(), kalkulusRegelGrunnlag.regelevaluering())
                && Objects.equals(fpsakSporing.getRegelInput(), kalkulusRegelGrunnlag.regelinput())
                && Objects.equals(fpsakSporing.getRegelVersjon(), kalkulusRegelGrunnlag.regelversjon());
        });
        if (!alleGrunnlagSporingerMatcher) {
            throw new IllegalStateException("Feil med matching av regelsporing på grunnlagsnivå");
        }

        // Verifiser periodesporinger
        var bgPerioder = entitet.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        var allePeriodeSporingerMatcher = response.sporingerPeriode().stream().allMatch(kalkulusRegelPeriode -> {
            var matchetBgPeriode = bgPerioder.stream()
                .filter(b -> b.getPeriode().getFomDato().equals(kalkulusRegelPeriode.periode().getFom()))
                .findFirst()
                .orElseThrow();
            BeregningsgrunnlagPeriodeRegelType fpsakType = KodeverkFraKalkulusMapper.mapRegelPeriodeType(kalkulusRegelPeriode.type());
            var fpsakSporing = matchetBgPeriode.getRegelSporinger().get(fpsakType);
            return fpsakSporing != null && fpsakSporing.getRegelEvaluering().equals(kalkulusRegelPeriode.regelevaluering())
                && fpsakSporing.getRegelInput().equals(kalkulusRegelPeriode.regelinput()) && Objects.equals(fpsakSporing.getRegelVersjon(),
                kalkulusRegelPeriode.regelversjon());
        });
        if (!allePeriodeSporingerMatcher) {
            throw new IllegalStateException("Feil med matching av regelsporing på periodenivå");
        }
    }

    private MigrerBeregningsgrunnlagRequest lagMigreringRequest(BehandlingReferanse behandlingReferanse, BeregningsgrunnlagKobling kobling,
                                                                Optional<BeregningsgrunnlagKobling> originalKobling,
                                                                BeregningsgrunnlagGrunnlagMigreringDto migreringsDto, boolean erAktiv) {
        var saksnummer = new Saksnummer(behandlingReferanse.saksnummer().getVerdi());
        var personIdent = new AktørIdPersonident(behandlingReferanse.aktørId().getId());
        var ytelse = mapYtelseSomSkalBeregnes(behandlingReferanse.fagsakYtelseType());
        return new MigrerBeregningsgrunnlagRequest(saksnummer, kobling.getKoblingUuid(), personIdent, ytelse, originalKobling.map(BeregningsgrunnlagKobling::getKoblingUuid).orElse(null), migreringsDto, erAktiv);
    }

    private no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType mapYtelseSomSkalBeregnes(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Ukjent ytelse som skal beregnes " + fagsakYtelseType);
        };
    }
}
