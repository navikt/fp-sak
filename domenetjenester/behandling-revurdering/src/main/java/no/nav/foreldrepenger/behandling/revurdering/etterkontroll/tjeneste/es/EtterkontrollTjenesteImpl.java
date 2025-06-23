package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.es;

import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class EtterkontrollTjenesteImpl implements EtterkontrollTjeneste {

    private Period pdlRegistreringsTidsrom;
    private RevurderingTjeneste revurderingTjeneste;

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private EngangsstønadBeregningRepository esBeregningRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    EtterkontrollTjenesteImpl() {
    }

    @Inject
    public EtterkontrollTjenesteImpl(BehandlingVedtakRepository vedtakRepository,
            EngangsstønadBeregningRepository esBeregningRepository,
            BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) RevurderingTjeneste revurderingTjeneste,
            @KonfigVerdi(value = "etterkontroll.pdlregistrering.periode", defaultVerdi = "P11W") Period pdlRegistreringsTidsrom) {
        this.pdlRegistreringsTidsrom = pdlRegistreringsTidsrom;
        this.behandlingVedtakRepository = vedtakRepository;
        this.revurderingTjeneste = revurderingTjeneste;
        this.esBeregningRepository = esBeregningRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    public Optional<BehandlingÅrsakType> utledRevurderingÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag,
            List<FødtBarnInfo> barnFraRegister) {
        if (grunnlag == null) {
            return Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
        }

        var utledetÅrsak = utledRevurderingsÅrsak(behandling, grunnlag, barnFraRegister.size());
        if (utledetÅrsak.isEmpty() && skalReberegneES(behandling, barnFraRegister)) {
            return Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
        }
        return utledetÅrsak;
    }

    @Override
    public void opprettRevurdering(Behandling behandling, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering) {
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(behandling.getFagsak(), årsak, enhetForRevurdering);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
    }

    private Optional<BehandlingÅrsakType> utledRevurderingsÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag,
            int antallBarnRegister) {
        var antallBarnSakBekreftet = finnAntallBekreftet(grunnlag);

        if (antallBarnRegister == 0 && finnAntallOverstyrtManglendeFødsel(grunnlag) > 0) {
            return Optional.empty();
        }
        if (antallBarnSakBekreftet > 0 && antallBarnSakBekreftet == antallBarnRegister) {
            return Optional.empty();
        }

        if (antallBarnRegister > 0) {
            return Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
        }

        var termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        if (termindato.isPresent()) {
            var tidligstePDLRegistreringsDato = termindato.get().minus(pdlRegistreringsTidsrom);
            var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
            var vedtaksDato = vedtak.getVedtaksdato();
            if (vedtaksDato.isBefore(tidligstePDLRegistreringsDato)) {
                return Optional.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);
            }
        }

        return Optional.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private boolean skalReberegneES(Behandling behandling, List<FødtBarnInfo> fødteBarn) {
        var fødselsdato = fødteBarn.stream().map(FødtBarnInfo::fødselsdato).max(Comparator.naturalOrder()).orElse(null);
        return fødselsdato != null && esBeregningRepository.skalReberegne(behandling.getId(), fødselsdato);
    }

    private int finnAntallBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0); // Inkluderer termin/overstyrt
    }

    private int finnAntallOverstyrtManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getOverstyrtVersjon().filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType())).map(FamilieHendelseEntitet::getAntallBarn)
                .orElse(0);
    }

}
