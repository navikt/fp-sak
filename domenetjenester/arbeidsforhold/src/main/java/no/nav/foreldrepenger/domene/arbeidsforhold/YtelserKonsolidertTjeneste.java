package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelserDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class YtelserKonsolidertTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(YtelserKonsolidertTjeneste.class);

    private static final Set<FagsakStatus> STATUS_ÅPEN = Set.of(FagsakStatus.OPPRETTET, FagsakStatus.UNDER_BEHANDLING);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    YtelserKonsolidertTjeneste() {
        // CDI
    }

    @Inject
    public YtelserKonsolidertTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    /**
     * Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til
     * behandling i VL (som ennå ikke har vedtak).
     */
    public List<TilgrensendeYtelserDto> utledYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Set<RelatertYtelseType> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
               .filter(ytelse -> inkluder.isEmpty() || inkluder.contains(ytelse.getRelatertYtelseType()))
                .filter(ytelse -> !(RelatertYtelseType.ENGANGSSTØNAD.equals(ytelse.getRelatertYtelseType()) && Fagsystem.FPSAK.equals(ytelse.getKilde())))
                .toList();
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        var saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        var iDag = LocalDate.now();
        var resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
                .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
                .filter(sak -> inkluder.isEmpty()
                        || inkluder.contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
                .filter(sak -> STATUS_ÅPEN.contains(sak.getStatus())
                    || FagsakYtelseType.ENGANGSTØNAD.equals(sak.getYtelseType()) && !erSisteBehandlingAvsluttetAvslag(sak))
                .map(sak -> mapFraFagsakForBruker(sak, iDag))
                .filter(Objects::nonNull)
                .toList();

        resultat.addAll(resultatÅpen);
        return resultat;
    }

    /**
     * Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til
     * behandling i VL (som ennå ikke har vedtak).
     */
    public List<TilgrensendeYtelserDto> utledAnnenPartsYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Set<RelatertYtelseType> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
                .filter(ytelse -> inkluder.isEmpty() || inkluder.contains(ytelse.getRelatertYtelseType()))
                .filter(ytelse -> !(RelatertYtelseType.ENGANGSSTØNAD.equals(ytelse.getRelatertYtelseType()) && Fagsystem.FPSAK.equals(ytelse.getKilde())))
                .toList();
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        var saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        var resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
                .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
                .filter(sak -> inkluder.isEmpty()
                        || inkluder.contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
                .filter(sak -> STATUS_ÅPEN.contains(sak.getStatus()) || !erSisteBehandlingAvsluttetAvslag(sak))
                .map(sak -> mapFraFagsakForAnnenPart(sak, sak.getOpprettetTidspunkt().toLocalDate()))
                .toList();

        resultat.addAll(resultatÅpen);
        return resultat;
    }

    private TilgrensendeYtelserDto mapFraFagsakForBruker(Fagsak fagsak, LocalDate periodeDato) {
        var tilgrensendeYtelserDto = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsak, periodeDato);
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            return setFamilieHendelseDatoForES(fagsak, tilgrensendeYtelserDto);
        }
        return tilgrensendeYtelserDto;
    }

    private TilgrensendeYtelserDto mapFraFagsakForAnnenPart(Fagsak fagsak, LocalDate periodeDato) {
        var tilgrensendeYtelserDto = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsak, periodeDato);
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            return setFamilieHendelseDatoForES(fagsak, tilgrensendeYtelserDto);
        }
        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).ifPresent(b -> {
            var min = tilkjentYtelseRepository.hentUtbetBeregningsresultat(b.getId())
                    .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                    .map(p -> VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()))
                    .min(Comparator.naturalOrder()).orElse(periodeDato);
            var max = tilkjentYtelseRepository.hentUtbetBeregningsresultat(b.getId())
                    .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                    .map(p -> VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()))
                    .max(Comparator.naturalOrder()).orElse(periodeDato);
            tilgrensendeYtelserDto.setPeriodeFraDato(min);
            tilgrensendeYtelserDto.setPeriodeTilDato(max);
        });
        return tilgrensendeYtelserDto;
    }

    private TilgrensendeYtelserDto setFamilieHendelseDatoForES(Fagsak fagsak, TilgrensendeYtelserDto tilgrensendeYtelserDto) {
        try {
            behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId())
                .flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()))
                .map(a -> a.getGjeldendeVersjon().getSkjæringstidspunkt())
                .ifPresent(d -> {
                    tilgrensendeYtelserDto.setPeriodeFraDato(d);
                    tilgrensendeYtelserDto.setPeriodeTilDato(d);
                });
        } catch (Exception e) {
            LOG.info("Ytelse konsolidert uten familiehendelsedato");
        }
        return tilgrensendeYtelserDto;
    }

    private boolean erSisteBehandlingAvsluttetAvslag(Fagsak fagsak) {
        var behandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId());
        return behandling.filter(Behandling::erSaksbehandlingAvsluttet).isPresent() &&
            behandling.flatMap(b -> behandlingsresultatRepository.hentHvisEksisterer(b.getId()))
                .map(Behandlingsresultat::getBehandlingResultatType)
                .filter(BehandlingResultatType.AVSLÅTT::equals).isPresent();
    }
}
