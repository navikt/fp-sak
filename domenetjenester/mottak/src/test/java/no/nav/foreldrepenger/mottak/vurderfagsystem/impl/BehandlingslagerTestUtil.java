package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class BehandlingslagerTestUtil {


    private BehandlingslagerTestUtil() {
    }

    public static final Fagsak buildFagsak(final Long fagsakid, final boolean erAvsluttet, FagsakYtelseType ytelseType) {
        var bruker = lagNavBruker();
        var fagsak = Fagsak.opprettNy(ytelseType, bruker, null, new Saksnummer(fagsakid * 2 + ""));
        fagsak.setId(fagsakid);
        if (erAvsluttet) {
            fagsak.setAvsluttet();
        }
        return fagsak;
    }

    public static final Fagsak buildFagsak(final Long fagsakid, final boolean erAvsluttet, FagsakYtelseType ytelseType, RelasjonsRolleType relasjonsRolleType) {
        var bruker = lagNavBruker();
        var fagsak = Fagsak.opprettNy(ytelseType, bruker, relasjonsRolleType, new Saksnummer(fagsakid * 2 + ""));
        fagsak.setId(fagsakid);
        if (erAvsluttet) {
            fagsak.setAvsluttet();
        }
        return fagsak;
    }

    public static final NavBruker lagNavBruker() {
        return NavBruker.opprettNyNB(AktørId.dummy());
    }

    public static final Behandling byggBehandlingFødsel(final Fagsak fagsakFødsel) {
        var behandlingBuilder = Behandling.forFørstegangssøknad(fagsakFødsel);
        return behandlingBuilder.build();
    }

    public static final FamilieHendelseGrunnlagEntitet byggFødselGrunnlag(LocalDate termindato, LocalDate fødselsdato) {
        var hendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        if (termindato != null) {
            hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medUtstedtDato(termindato.minusDays(40))
                .medTermindato(termindato)
                .medNavnPå("NAVN"));
        }
        if (fødselsdato != null) {
            hendelseBuilder.medFødselsDato(fødselsdato);
        }
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(hendelseBuilder)
            .build();
    }

}
