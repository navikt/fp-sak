package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelserDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class YtelserKonsolidertTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository tilkjentYtelseRepository;

    YtelserKonsolidertTjeneste() {
        // CDI
    }

    @Inject
    public YtelserKonsolidertTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tilkjentYtelseRepository = repositoryProvider.getBeregningsresultatRepository();
    }


    /** Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til behandling i VL (som ennå ikke har vedtak). */
    public List<TilgrensendeYtelserDto> utledYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Optional<Set<RelatertYtelseType>> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
            .filter(ytelse -> inkluder.isEmpty() || inkluder.get().contains(ytelse.getRelatertYtelseType()))
            .collect(Collectors.toList());
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        Set<Saksnummer> saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        LocalDate iDag = FPDateUtil.iDag();
        Set<FagsakStatus> statuser = Set.of(FagsakStatus.OPPRETTET, FagsakStatus.UNDER_BEHANDLING);
        List<TilgrensendeYtelserDto> resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
            .filter(sak -> inkluder.isEmpty() || inkluder.get().contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
            .filter(sak -> statuser.contains(sak.getStatus()))
            .map(sak -> BehandlingRelaterteYtelserMapper.mapFraFagsak(sak, iDag))
            .collect(Collectors.toList());

        resultat.addAll(resultatÅpen);
        return resultat;
    }

    /** Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til behandling i VL (som ennå ikke har vedtak). */
    public List<TilgrensendeYtelserDto> utledAnnenPartsYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Optional<Set<RelatertYtelseType>> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
            .filter(ytelse -> !inkluder.isPresent() || inkluder.get().contains(ytelse.getRelatertYtelseType()))
            .collect(Collectors.toList());
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        Set<Saksnummer> saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        List<TilgrensendeYtelserDto> resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
            .filter(sak -> !inkluder.isPresent() || inkluder.get().contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
            .map(sak -> mapFraFagsakMedPeriode(sak, sak.getOpprettetTidspunkt().toLocalDate()))
            .collect(Collectors.toList());

        resultat.addAll(resultatÅpen);
        return resultat;
    }

    private TilgrensendeYtelserDto mapFraFagsakMedPeriode(Fagsak fagsak, LocalDate periodeDato) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = BehandlingRelaterteYtelserMapper.mapFraFagsak(fagsak, periodeDato);
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType()))
            return tilgrensendeYtelserDto;
        behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).ifPresent(b -> {
            var min = tilkjentYtelseRepository.hentBeregningsresultat(b.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .map(p -> VirkedagUtil.fomVirkedag(p.getBeregningsresultatPeriodeFom()))
                .min(Comparator.naturalOrder()).orElse(periodeDato);
            var max = tilkjentYtelseRepository.hentBeregningsresultat(b.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .map(p -> VirkedagUtil.tomVirkedag(p.getBeregningsresultatPeriodeTom()))
                .max(Comparator.naturalOrder()).orElse(periodeDato);
            tilgrensendeYtelserDto.setPeriodeFraDato(min);
            tilgrensendeYtelserDto.setPeriodeTilDato(max);
        });
        return tilgrensendeYtelserDto;
    }
}
