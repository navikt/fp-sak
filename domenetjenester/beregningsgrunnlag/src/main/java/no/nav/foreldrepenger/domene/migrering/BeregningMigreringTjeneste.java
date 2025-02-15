package no.nav.foreldrepenger.domene.migrering;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;

import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KodeverkFraKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulus.migrering.BeregningsgrunnlagGrunnlagMigreringDto;
import no.nav.folketrygdloven.kalkulus.migrering.MigrerBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.migrering.MigrerBeregningsgrunnlagResponse;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.prosess.KalkulusKlient;

@ApplicationScoped
public class BeregningMigreringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningMigreringTjeneste.class);

    private KalkulusKlient klient;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    private BehandlingRepository behandlingRepository;

    BeregningMigreringTjeneste() {
        // CDI
    }

    @Inject
    public BeregningMigreringTjeneste(KalkulusKlient klient,
                                      BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      BeregningsgrunnlagKoblingRepository koblingRepository,
                                      BehandlingRepository behandlingRepository) {
        this.klient = klient;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.koblingRepository = koblingRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public void migrerSak(no.nav.foreldrepenger.domene.typer.Saksnummer saksnummer) {
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .sorted(Comparator.comparing(Behandling::getOpprettetDato))
            .toList();
        behandlinger.forEach(b -> migrerBehandling(BehandlingReferanse.fra(b)));
    }

    private void migrerBehandling(BehandlingReferanse referanse) {
        if (erAlleredeMigrert(referanse)) {
            LOG.info(String.format("Behandling %s er allerede migrert.", referanse.behandlingId()));
            return;
        }
        var grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId());
        if (grunnlag.isEmpty()) {
            LOG.info(String.format("Finner ikke beregningsgrunnlag på behandling %s, ingenting å migrere", referanse.behandlingId()));
            return;
        }
        try {
            var originalKobling = referanse.getOriginalBehandlingId().flatMap(oid -> koblingRepository.hentKobling(oid));
            var migreringsDto = BeregningMigreringMapper.map(grunnlag.get());
            var kobling = koblingRepository.opprettKobling(referanse);
            var request = lagMigreringRequest(referanse, kobling, originalKobling, migreringsDto);
            var response = klient.migrerGrunnlag(request);
            sammenlignGrunnlag(response, referanse);
            LOG.info(String.format("Vellykket migrering og verifisering av beregningsgrunnlag på sak %s, behandlingId %s og grunnlag %s.", referanse.saksnummer(),
                referanse.behandlingId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId)));
        } catch (Exception e) {
            var msg = String.format("Feil ved mapping av grunnlag for sak %s, behandlingId %s og grunnlag %s. Fikk feil %s", referanse.saksnummer(),
                referanse.behandlingId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId), e);
            throw new IllegalStateException(msg);
        }
    }

    private void sammenlignGrunnlag(MigrerBeregningsgrunnlagResponse response, BehandlingReferanse referanse) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId()).orElseThrow();
        verifiserRegelsporinger(response, entitet);
        var fpsakGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(entitet);
        var kalkulusGrunnlag = KalkulusTilFpsakMapper.map(response.grunnlag(), Optional.ofNullable(response.besteberegningGrunnlag()));
        var fpJson = StandardJsonConfig.toJson(fpsakGrunnlag);
        var kalkJson = StandardJsonConfig.toJson(kalkulusGrunnlag);
        if (!fpJson.equals(kalkJson)) {
            throw new IllegalStateException("Missmatch mellom kalkulus json og fpsak json av samme beregninsgrunnlag");
        }
    }

    private void verifiserRegelsporinger(MigrerBeregningsgrunnlagResponse response, BeregningsgrunnlagGrunnlagEntitet entitet) {
        // Verifiser grunnlagsporinger
        var grunnlagSporinger = response.sporingerGrunnlag();
        var fpsakGrunnlagSporinger = entitet.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getRegelSporinger)
            .orElse(Collections.emptyMap());
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

    private boolean erAlleredeMigrert(BehandlingReferanse referanse) {
        return koblingRepository.hentKobling(referanse.behandlingId()).isPresent();
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
