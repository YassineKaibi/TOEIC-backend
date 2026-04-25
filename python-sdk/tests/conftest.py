import pytest

from toeic_client import ToeicClient, AsyncToeicClient


@pytest.fixture
def client(httpx_mock):
    with ToeicClient(base_url="http://test/api/v1", token="fake-token") as c:
        yield c


@pytest.fixture
def async_client(httpx_mock):
    # AsyncToeicClient is constructed synchronously; the event loop
    # is managed by pytest-anyio per test.
    return AsyncToeicClient(base_url="http://test/api/v1", token="fake-token")
