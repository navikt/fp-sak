package no.nav.foreldrepenger.domene.migrering;

import java.util.Comparator;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.FpkalkulusYtelser;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagGrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.MigrerBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.MigrerBeregningsgrunnlagResponse;
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

        } catch (Exception e) {
            var msg = String.format("Feil ved mapping av grunnlag for sak %s, behandlingId %s og grunnlag %s. Fikk feil %s", referanse.saksnummer(),
                referanse.behandlingId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId), e);
            throw new IllegalStateException(msg);
        }
    }

    private void sammenlignGrunnlag(MigrerBeregningsgrunnlagResponse response, BehandlingReferanse referanse) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId()).orElseThrow();
        var fpsakGrunnlag = FraEntitetTilBehandlingsmodellMapper.mapBeregningsgrunnlagGrunnlag(entitet);
        var kalkulusGrunnlag = KalkulusTilFpsakMapper.map(response.grunnlag(), Optional.ofNullable(response.besteberegningGrunnlag()));
        var fpJson = StandardJsonConfig.toJson(fpsakGrunnlag);
        var kalkJson = StandardJsonConfig.toJson(kalkulusGrunnlag);
        if (fpJson.equals(kalkJson)) {
            LOG.info("Det er likt!");
        } else {
            LOG.info("Det er ulikt!");
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

    private FpkalkulusYtelser mapYtelseSomSkalBeregnes(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> FpkalkulusYtelser.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> FpkalkulusYtelser.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Ukjent ytelse som skal beregnes " + fagsakYtelseType);
        };
    }

}
