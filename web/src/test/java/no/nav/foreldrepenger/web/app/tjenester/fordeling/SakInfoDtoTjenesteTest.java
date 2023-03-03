package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    private FørsteUttaksdatoTjeneste førsteUttaksdatoTjenesteMock;

    private SakInfoDtoTjeneste sakInfoDtoTjeneste;

    @BeforeEach
    void setUp() {
        sakInfoDtoTjeneste = new SakInfoDtoTjeneste(behandlingRepositoryMock, førsteUttaksdatoTjenesteMock, familieHendelseRepositoryMock );
    }

    @Test
    void skalMappeSakInfoDtoMedAlleVerdier() {
        var saknr = new Saksnummer("TEST3");
        var opprettetTidSak1 = LocalDateTime.now().minusMonths(16);
        var skjæringstidspunkt = LocalDate.now().minusMonths(15);
        var førsteuttaksdato = LocalDate.now().minusMonths(6);


        Fagsak fagsak= Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak.setOpprettetTidspunkt(opprettetTidSak1);
        fagsak.setId(125L);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(behandling));
        when(førsteUttaksdatoTjenesteMock.finnFørsteUttaksdato(behandling)).thenReturn(Optional.of(førsteuttaksdato));
        when(familieHendelseRepositoryMock.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(familieHendelseGrunnlagEntitetMock));
        when(familieHendelseGrunnlagEntitetMock.getGjeldendeVersjon()).thenReturn(familieHendelseEntitetMock);
        when(familieHendelseEntitetMock.getSkjæringstidspunkt()).thenReturn(skjæringstidspunkt);
        when(familieHendelseEntitetMock.getType()).thenReturn(FamilieHendelseType.FØDSEL);

        var sakInfoDto = sakInfoDtoTjeneste.mapSakInfoDto(fagsak);

        assertThat(sakInfoDto.saksnummer().getSaksnummer()).isEqualTo(saknr.getVerdi());
        assertThat(sakInfoDto.status()).isEqualTo(sakInfoDtoTjeneste.mapFagsakStatusDto(FagsakStatus.OPPRETTET));
        assertThat(sakInfoDto.opprettetDato()).isEqualTo(opprettetTidSak1.toLocalDate());
        assertThat(sakInfoDto.ytelseType()).isEqualTo(sakInfoDtoTjeneste.mapFagsakYtelseTypeDto(FagsakYtelseType.FORELDREPENGER));
        assertThat(sakInfoDto.familiehendelseInfoDto().familiehendelseDato()).isEqualTo(skjæringstidspunkt);
        assertThat(sakInfoDto.familiehendelseInfoDto().familihendelseType()).isEqualTo(sakInfoDtoTjeneste.mapFamilieHendelseTypeDto(FamilieHendelseType.FØDSEL));
        assertThat(sakInfoDto.førsteUttaksdato()).isEqualTo(førsteuttaksdato);
    }

    @Test
    void skalMappeSakInfoDtoMenNullVerdier() {
        var saknr = new Saksnummer("TEST4");
        var ytelseType = FagsakYtelseType.FORELDREPENGER;
        var opprettetTid = LocalDateTime.now().minusMonths(16);


        Fagsak fagsak= Fagsak.opprettNy(ytelseType, NavBruker.opprettNy(AKTØR_ID_MOR, Språkkode.NB), saknr);
        fagsak.setOpprettetTidspunkt(opprettetTid);
        fagsak.setId(125L);

        when(behandlingRepositoryMock.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.empty());

        var sakInfoDto = sakInfoDtoTjeneste.mapSakInfoDto(fagsak);

        assertThat(sakInfoDto.saksnummer().getSaksnummer()).isEqualTo(saknr.getVerdi());
        assertThat(sakInfoDto.status()).isEqualTo(sakInfoDtoTjeneste.mapFagsakStatusDto(FagsakStatus.OPPRETTET));
        assertThat(sakInfoDto.opprettetDato()).isEqualTo(opprettetTid.toLocalDate());
        assertThat(sakInfoDto.ytelseType()).isEqualTo(sakInfoDtoTjeneste.mapFagsakYtelseTypeDto(ytelseType));
        assertThat(sakInfoDto.familiehendelseInfoDto()).isNull();
        assertThat(sakInfoDto.førsteUttaksdato()).isNull();
    }
}
