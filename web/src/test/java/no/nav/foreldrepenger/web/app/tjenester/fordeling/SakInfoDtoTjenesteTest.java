package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.SakInfoV2Dto;
import no.nav.foreldrepenger.kontrakter.fordel.YtelseTypeDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ExtendWith(MockitoExtension.class)
class SakInfoDtoTjenesteTest {
    private static final AktørId AKTØR_ID_MOR = AktørId.dummy();
    @Mock
    private FamilieHendelseRepository familieHendelseRepositoryMock;
    @Mock
    private FamilieHendelseGrunnlagEntitet familieHendelseGrunnlagEntitetMock;
    @Mock
    private BehandlingRepository behandlingRepositoryMock;
    @Mock
    private FamilieHendelseEntitet familieHendelseEntitetMock;
    @Mock
    private SkjæringstidspunktTjeneste skjæringstidspunktTjenesteMock;
    @Mock
    private Skjæringstidspunkt skjæringstidspunktMock;
    private SakInfoDtoTjeneste sakInfoDtoTjeneste;

    @BeforeEach
    void setUp() {
        sakInfoDtoTjeneste = new SakInfoDtoTjeneste(behandlingRepositoryMock, familieHendelseRepositoryMock, skjæringstidspunktTjenesteMock );
    }

    @Test
    void skalMappeFPSakInfoDtoMedAlleVerdier() {
        var saknr = new Saksnummer("TEST3");
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);
        var førsteuttaksdato = LocalDate.now().minusMonths(6);


        var fagsak= Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak.setOpprettetTidspunkt(opprettetTidSak1);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(behandling));
        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(behandling.getId())).thenReturn(skjæringstidspunktMock);
        when(skjæringstidspunktMock.getFørsteUttaksdatoHvisFinnes()).thenReturn(Optional.of(førsteuttaksdato));
        when(familieHendelseRepositoryMock.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlagEntitetMock));
        when(familieHendelseGrunnlagEntitetMock.getGjeldendeVersjon()).thenReturn(familieHendelseEntitetMock);
        when(familieHendelseEntitetMock.getSkjæringstidspunkt()).thenReturn(skjæringstidspunkt);
        when(familieHendelseEntitetMock.getType()).thenReturn(FamilieHendelseType.FØDSEL);

        var sakInfoDto = sakInfoDtoTjeneste.mapSakInfoV2Dto(fagsak);

        assertThat(sakInfoDto.saksnummer().getSaksnummer()).isEqualTo(saknr.getVerdi());
        assertThat(sakInfoDto.status()).isEqualTo(SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING);
        assertThat(sakInfoDto.opprettetDato()).isEqualTo(opprettetTidSak1.toLocalDate());
        assertThat(sakInfoDto.ytelseType()).isEqualTo(YtelseTypeDto.FORELDREPENGER);
        assertThat(sakInfoDto.familiehendelseInfoDto().familiehendelseDato()).isEqualTo(skjæringstidspunkt);
        assertThat(sakInfoDto.familiehendelseInfoDto().familihendelseType()).isEqualTo(SakInfoV2Dto.FamilieHendelseTypeDto.FØDSEL);
        assertThat(sakInfoDto.førsteUttaksdato()).isEqualTo(førsteuttaksdato);
    }

    @Test
    void skalMappeSVPSakInfoDtoMedAlleVerdier() {
        var saknr = new Saksnummer("TEST3");
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);
        var førsteuttaksdato = LocalDate.now().minusMonths(6);


        var fagsak= Fagsak.opprettNy(FagsakYtelseType.SVANGERSKAPSPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak.setOpprettetTidspunkt(opprettetTidSak1);

        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(behandling));
        when(familieHendelseRepositoryMock.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlagEntitetMock));
        when(familieHendelseGrunnlagEntitetMock.getGjeldendeVersjon()).thenReturn(familieHendelseEntitetMock);
        when(familieHendelseEntitetMock.getSkjæringstidspunkt()).thenReturn(skjæringstidspunkt);
        when(familieHendelseEntitetMock.getType()).thenReturn(FamilieHendelseType.FØDSEL);

        when(skjæringstidspunktTjenesteMock.getSkjæringstidspunkter(behandling.getId())).thenReturn(skjæringstidspunktMock);
        when(skjæringstidspunktMock.getFørsteUttaksdatoHvisFinnes()).thenReturn(Optional.empty());
        when(skjæringstidspunktMock.getSkjæringstidspunktHvisUtledet()).thenReturn(Optional.of(førsteuttaksdato));

        var sakInfoDto = sakInfoDtoTjeneste.mapSakInfoV2Dto(fagsak);

        assertThat(sakInfoDto.saksnummer().getSaksnummer()).isEqualTo(saknr.getVerdi());
        assertThat(sakInfoDto.status()).isEqualTo(SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING);
        assertThat(sakInfoDto.opprettetDato()).isEqualTo(opprettetTidSak1.toLocalDate());
        assertThat(sakInfoDto.ytelseType()).isEqualTo(YtelseTypeDto.SVANGERSKAPSPENGER);
        assertThat(sakInfoDto.familiehendelseInfoDto().familiehendelseDato()).isEqualTo(skjæringstidspunkt);
        assertThat(sakInfoDto.familiehendelseInfoDto().familihendelseType()).isEqualTo(SakInfoV2Dto.FamilieHendelseTypeDto.FØDSEL);
        assertThat(sakInfoDto.førsteUttaksdato()).isEqualTo(førsteuttaksdato);
    }

    @Test
    void skalMappeSakInfoV2DtoMedNullVerdier() {
        var saknr = new Saksnummer("TEST4");
        var ytelseType = FagsakYtelseType.FORELDREPENGER;
        var opprettetTid = LocalDateTime.now().minusMonths(16);

        var fagsak = Fagsak.opprettNy(ytelseType, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak.setOpprettetTidspunkt(opprettetTid);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.empty());

        var sakInfoDto = sakInfoDtoTjeneste.mapSakInfoV2Dto(fagsak);

        assertThat(sakInfoDto.saksnummer().getSaksnummer()).isEqualTo(saknr.getVerdi());
        assertThat(sakInfoDto.status()).isEqualTo(SakInfoV2Dto.FagsakStatusDto.UNDER_BEHANDLING);
        assertThat(sakInfoDto.opprettetDato()).isEqualTo(opprettetTid.toLocalDate());
        assertThat(sakInfoDto.ytelseType()).isEqualTo(YtelseTypeDto.FORELDREPENGER);
        assertThat(sakInfoDto.familiehendelseInfoDto()).isNull();
        assertThat(sakInfoDto.førsteUttaksdato()).isNull();
    }
}
