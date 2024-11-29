package no.nav.foreldrepenger.domene.migrering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.FpkalkulusYtelser;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.BeregningsgrunnlagGrunnlagMigreringDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.MigrerBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.prosess.KalkulusKlient;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class BeregningMigreringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningMigreringTjeneste.class);

    private KalkulusKlient klient;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagKoblingRepository koblingRepository;

    BeregningMigreringTjeneste() {
        // CDI
    }

    @Inject
    public BeregningMigreringTjeneste(KalkulusKlient klient,
                                      BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      BeregningsgrunnlagKoblingRepository koblingRepository) {
        this.klient = klient;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.koblingRepository = koblingRepository;
    }

    public void migrerBehandling(BehandlingReferanse referanse) {
        if (erAlleredeMigrert(referanse)) {
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
            klient.migrerGrunnlag(request);
        } catch (Exception e) {
            var msg = String.format("Feil ved mapping av grunnlag for sak %s, behandlingId %s og grunnlag %s", referanse.saksnummer(),
                referanse.behandlingId(), grunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getId));
            throw new IllegalStateException(msg);
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
