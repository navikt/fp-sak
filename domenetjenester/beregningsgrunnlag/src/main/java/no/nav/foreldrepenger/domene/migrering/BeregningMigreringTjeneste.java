package no.nav.foreldrepenger.domene.migrering;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

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
import no.nav.foreldrepenger.domene.prosess.GrunnbeløpReguleringsutleder;
import no.nav.foreldrepenger.domene.prosess.KalkulusKlient;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class BeregningMigreringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningMigreringTjeneste.class);

    private KalkulusKlient klient;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    private BehandlingRepository behandlingRepository;
    private RegelsporingMigreringTjeneste regelsporingMigreringTjeneste;

    BeregningMigreringTjeneste() {
        // CDI
    }

    @Inject
    public BeregningMigreringTjeneste(KalkulusKlient klient,
                                      BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      BeregningsgrunnlagKoblingRepository koblingRepository,
                                      BehandlingRepository behandlingRepository,
                                      RegelsporingMigreringTjeneste regelsporingMigreringTjeneste) {
        this.klient = klient;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.koblingRepository = koblingRepository;
        this.behandlingRepository = behandlingRepository;
        this.regelsporingMigreringTjeneste = regelsporingMigreringTjeneste;
    }

    public void migrerSak(no.nav.foreldrepenger.domene.typer.Saksnummer saksnummer) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(Behandling::erAvsluttet)
            .sorted(Comparator.comparing(Behandling::getOpprettetDato))
            .toList();
        behandlinger.forEach(b -> migrerBehandling(BehandlingReferanse.fra(b)));
    }

    public boolean kanHentesFraKalkulus(BehandlingReferanse behandlingReferanse) {
        return koblingRepository.hentKobling(behandlingReferanse.behandlingId()).isPresent();

    }

    private void migrerBehandling(BehandlingReferanse referanse) {
        var grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId());
        if (grunnlag.isEmpty()) {
            LOG.info(String.format("Finner ikke beregningsgrunnlag på behandling %s, ingenting å migrere", referanse.behandlingId()));
            return;
        }
        try {
            // Map og migrer
            var grunnlagSporinger = regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(grunnlag.get(),
                referanse);
            var originalKobling = referanse.getOriginalBehandlingId().flatMap(oid -> koblingRepository.hentKobling(oid));
            var aksjonspunkter = behandlingRepository.hentBehandling(referanse.behandlingId()).getAksjonspunkter();
            var migreringsDto = BeregningMigreringMapper.map(grunnlag.get(), grunnlagSporinger, aksjonspunkter);
            var kobling = koblingRepository.hentKobling(referanse.behandlingId()).orElseGet(() -> koblingRepository.opprettKobling(referanse));
            var request = lagMigreringRequest(referanse, kobling, originalKobling, migreringsDto);
            var response = klient.migrerGrunnlag(request);

            // Sammenlign grunnlag fra kalkulus og fpsak
            sammenlignGrunnlag(response, referanse, grunnlagSporinger, aksjonspunkter);

            // Oppdater kobling med data fra grunnlag
            if (response.grunnlag() != null && response.grunnlag().getBeregningsgrunnlag() != null) {
                oppdaterKoblingMedStpGrunnbeløpOgReguleringsbehov(kobling, response.grunnlag().getBeregningsgrunnlag());
            }

            LOG.info(String.format("Vellykket migrering og verifisering av beregningsgrunnlag på sak %s, behandlingId %s og grunnlag %s.", referanse.saksnummer(),
                referanse.behandlingId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId)));
        } catch (Exception e) {
            var msg = String.format("Feil ved mapping av grunnlag for sak %s, behandlingId %s og grunnlag %s. Fikk feil %s", referanse.saksnummer(),
                referanse.behandlingUuid(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId), e);
            throw new IllegalStateException(msg);
        }
    }

    private void oppdaterKoblingMedStpGrunnbeløpOgReguleringsbehov(BeregningsgrunnlagKobling kobling, BeregningsgrunnlagDto grunnlag) {
        var stp = grunnlag.getSkjæringstidspunkt();
        var grunnbeløp = grunnlag.getGrunnbeløp() == null ? null : Beløp.fra(grunnlag.getGrunnbeløp().verdi());
        var harBehovForGRegulering = GrunnbeløpReguleringsutleder.kanPåvirkesAvGrunnbeløpRegulering(KalkulusTilFpsakMapper.mapGrunnlag(grunnlag, Optional.empty()));
        koblingRepository.oppdaterKoblingMedStpOgGrunnbeløp(kobling, grunnbeløp, stp);
        koblingRepository.oppdaterKoblingMedReguleringsbehov(kobling, harBehovForGRegulering);
    }

    private void sammenlignGrunnlag(MigrerBeregningsgrunnlagResponse response, BehandlingReferanse referanse,
                                    Map<BeregningsgrunnlagRegelType, BeregningsgrunnlagRegelSporing> grunnlagSporinger,
                                    Set<Aksjonspunkt> aksjonspunkter) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId()).orElseThrow();
        verifiserRegelsporinger(response, entitet, grunnlagSporinger);
        verifiserAksjonspunkter(aksjonspunkter, response.avklaringsbehov());
        var fpsakGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(entitet);
        var kalkulusGrunnlag = KalkulusTilFpsakMapper.map(response.grunnlag(), Optional.ofNullable(response.besteberegningGrunnlag()));
        var fpJson = StandardJsonConfig.toJson(fpsakGrunnlag);
        var kalkJson = StandardJsonConfig.toJson(kalkulusGrunnlag);
        if (!fpJson.equals(kalkJson)) {
            logg(fpJson, kalkJson);
            throw new IllegalStateException("Missmatch mellom kalkulus json og fpsak json av samme beregninsgrunnlag");
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

    private void logg(String jsonFpsak, String jsonKalkulus) {
        try {
            var maskertFpsak = jsonFpsak.replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            var maskertKalkulus = jsonKalkulus.replaceAll("\\d{13}|\\d{11}|\\d{9}", "*");
            LOG.info("Json fpsak: {} Json kalkulus: {} ", maskertFpsak, maskertKalkulus);
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
            return fpsakSporing != null && fpsakSporing.getRegelEvaluering().equals(kalkulusRegelGrunnlag.regelevaluering()) && fpsakSporing.getRegelInput()
                .equals(kalkulusRegelGrunnlag.regelinput()) && Objects.equals(fpsakSporing.getRegelVersjon(), kalkulusRegelGrunnlag.regelversjon());
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
                                                                BeregningsgrunnlagGrunnlagMigreringDto migreringsDto) {
        var saksnummer = new Saksnummer(behandlingReferanse.saksnummer().getVerdi());
        var personIdent = new AktørIdPersonident(behandlingReferanse.aktørId().getId());
        var ytelse = mapYtelseSomSkalBeregnes(behandlingReferanse.fagsakYtelseType());
        return new MigrerBeregningsgrunnlagRequest(saksnummer, kobling.getKoblingUuid(), personIdent, ytelse, originalKobling.map(BeregningsgrunnlagKobling::getKoblingUuid).orElse(null), migreringsDto);
    }

    private no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType mapYtelseSomSkalBeregnes(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Ukjent ytelse som skal beregnes " + fagsakYtelseType);
        };
    }
}
