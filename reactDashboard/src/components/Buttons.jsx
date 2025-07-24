import React from 'react';
import { OnOffIcon, BulbIcon } from '../assets/Icons';

function ButtonOnOff({ onClick, classNameStatus, label = "ON" }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command ${classNameStatus}`}>
                <OnOffIcon fill="#ffffff" />
            </button>
            <p>{label}</p>
        </div>
    );
}

export function ButtonOn({ onClick }) {
    return <ButtonOnOff onClick={onClick} classNameStatus="button-on" label="ON" />;
}

export function ButtonOff({ onClick }) {
    return <ButtonOnOff onClick={onClick} classNameStatus="button-off" label="OFF" />;
}

function ButtonLight({ onClick, classNameStatus, label = "ALZA" }) {
    return (
        <div className="button-description">
            <button onClick={onClick} className={`button-command ${classNameStatus}`}>
                <BulbIcon />
            </button>
            <p>{label}</p>
        </div>
    );
}

export function ButtonLightDown({ onClick }) {
    return <ButtonLight onClick={onClick} classNameStatus="button-50" label="ABBASSA" />;
}

export function ButtonLightUp({ onClick }) {
    return <ButtonLight onClick={onClick} classNameStatus="button-100" label="ALZA" />;
}
