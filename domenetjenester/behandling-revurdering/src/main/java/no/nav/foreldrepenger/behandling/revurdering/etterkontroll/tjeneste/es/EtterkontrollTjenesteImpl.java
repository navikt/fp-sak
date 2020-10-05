package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.es;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class EtterkontrollTjenesteImpl implements EtterkontrollTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(EtterkontrollTjenesteImpl.class);
    private Period tpsRegistreringsTidsrom;
    private RevurderingTjeneste revurderingTjeneste;

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private LegacyESBeregningRepository esBeregningRepository;
    private ProsessTaskRepository prosessTaskRepository;


    EtterkontrollTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public EtterkontrollTjenesteImpl(BehandlingVedtakRepository vedtakRepository,
                                     LegacyESBeregningRepository esBeregningRepository,
                                     ProsessTaskRepository prosessTaskRepository,
                                     @FagsakYtelseTypeRef("ES") RevurderingTjeneste revurderingTjeneste,
                                     @KonfigVerdi(value = "etterkontroll.tpsregistrering.periode", defaultVerdi = "P11W") Period tpsRegistreringsTidsrom) {
        this.tpsRegistreringsTidsrom = tpsRegistreringsTidsrom;
        this.behandlingVedtakRepository = vedtakRepository;
        this.revurderingTjeneste = revurderingTjeneste;
        this.esBeregningRepository = esBeregningRepository;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public Optional<BehandlingÅrsakType> utledRevurderingÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag, List<FødtBarnInfo> barnFraRegister) {
        if (grunnlag == null)
            return Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);

        Optional<BehandlingÅrsakType> utledetÅrsak = utledRevurderingsÅrsak(behandling, grunnlag, barnFraRegister.size());
        if (utledetÅrsak.isEmpty() && skalReberegneES(behandling, barnFraRegister)) {
            return Optional.of(BehandlingÅrsakType.RE_SATS_REGULERING);
        }
        return utledetÅrsak;
    }

    @Override
    public void opprettRevurdering(Behandling behandling, BehandlingÅrsakType årsak, OrganisasjonsEnhet enhetForRevurdering) {
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(behandling.getFagsak(), årsak, enhetForRevurdering);
        opprettTaskForProsesserBehandling(revurdering);
    }

    private Optional<BehandlingÅrsakType> utledRevurderingsÅrsak(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag, int antallBarnRegister) {
        int antallBarnSakBekreftet = finnAntallBekreftet(grunnlag);

        if (antallBarnRegister == 0 && finnAntallOverstyrtManglendeFødsel(grunnlag) > 0) {
            return Optional.empty();
        }
        if (antallBarnSakBekreftet > 0 && antallBarnSakBekreftet == antallBarnRegister) {
            return Optional.empty();
        }

        if (antallBarnRegister > 0) {
            return Optional.of(BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
        }

        Optional<LocalDate> termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        if (termindato.isPresent()) {
            LocalDate tidligsteTpsRegistreringsDato = termindato.get().minus(tpsRegistreringsTidsrom);
            var vedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
            LocalDate vedtaksDato = vedtak.getVedtaksdato();
            if (vedtaksDato.isBefore(tidligsteTpsRegistreringsDato)) {
                return Optional.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE);
            }
        }

        return Optional.of(BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private boolean skalReberegneES(Behandling behandling, List<FødtBarnInfo> fødteBarn) {
        var fødselsdato = fødteBarn.stream().map(FødtBarnInfo::getFødselsdato).max(Comparator.naturalOrder()).orElse(null);
        return fødselsdato != null && esBeregningRepository.skalReberegne(behandling.getId(), fødselsdato);
    }

    private int finnAntallBekreftet(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getGjeldendeBekreftetVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0); // Inkluderer termin/overstyrt
    }

    private int finnAntallOverstyrtManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlag) {
        return grunnlag.getOverstyrtVersjon().filter(fh -> FamilieHendelseType.FØDSEL.equals(fh.getType())).map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
    }

    private void opprettTaskForProsesserBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

}
