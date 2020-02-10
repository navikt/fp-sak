package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.TilgrensendeYtelserDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class YtelserKonsolidertTjeneste {

    private FagsakRepository fagsakRepository;

    YtelserKonsolidertTjeneste() {
        // CDI
    }

    @Inject
    public YtelserKonsolidertTjeneste(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }


    /** Sammenstilt informasjon om vedtatte ytelser fra grunnlag og saker til behandling i VL (som ennå ikke har vedtak). */
    public List<TilgrensendeYtelserDto> utledYtelserRelatertTilBehandling(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag, Optional<Set<RelatertYtelseType>> inkluder) {

        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var ytelser = filter.getFiltrertYtelser();

        Collection<Ytelse> fraGrunnlag = ytelser.stream()
            .filter(ytelse -> !inkluder.isPresent() || inkluder.get().contains(ytelse.getRelatertYtelseType()))
            .collect(Collectors.toList());
        List<TilgrensendeYtelserDto> resultat = new ArrayList<>(BehandlingRelaterteYtelserMapper.mapFraBehandlingRelaterteYtelser(fraGrunnlag));

        Set<Saksnummer> saksnumre = fraGrunnlag.stream().map(Ytelse::getSaksnummer).filter(Objects::nonNull).collect(Collectors.toSet());
        LocalDate iDag = FPDateUtil.iDag();
        Set<FagsakStatus> statuser = Set.of(FagsakStatus.OPPRETTET, FagsakStatus.UNDER_BEHANDLING);
        List<TilgrensendeYtelserDto> resultatÅpen = fagsakRepository.hentForBruker(aktørId).stream()
            .filter(sak -> !saksnumre.contains(sak.getSaksnummer()))
            .filter(sak -> !inkluder.isPresent() || inkluder.get().contains(BehandlingRelaterteYtelserMapper.mapFraFagsakYtelseTypeTilRelatertYtelseType(sak.getYtelseType())))
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
            .map(sak -> BehandlingRelaterteYtelserMapper.mapFraFagsak(sak, sak.getOpprettetTidspunkt().toLocalDate()))
            .collect(Collectors.toList());

        resultat.addAll(resultatÅpen);
        return resultat;
    }
}
